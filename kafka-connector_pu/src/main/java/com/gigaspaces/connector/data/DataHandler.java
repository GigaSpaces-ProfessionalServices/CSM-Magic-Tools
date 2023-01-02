package com.gigaspaces.connector.data;

import com.gigaspaces.client.ChangeResult;
import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.connector.cdc.CdcException;
import com.gigaspaces.connector.cdc.CdcOperation;
import com.gigaspaces.connector.cdc.CdcOperationResolver;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.connector.plugins.data.Parser;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.query.IdQuery;
import com.gigaspaces.query.IdsQuery;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import oshi.util.tuples.Pair;

import java.io.Serializable;
import java.util.List;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class DataHandler {

    public static final Logger logger = LoggerFactory.getLogger(DataHandler.class);
    @Autowired
    private GigaSpace space;
    @Autowired
    private MetadataHandler metadataHandler;
    @Autowired
    private CdcOperationResolver cdcOperationResolver;
    @Autowired
    private PropertiesSetter propertiesSetter;
    @Autowired
    private ValueToSpaceTypeConverter valueToSpaceTypeConverter;
    @Autowired
    private MessageContainerFactory messageContainerFactory;

    public void handle(String topic, String kafkaMessageKey, String kafkaMessageValue) {
        if (kafkaMessageValue == null) {
            logger.debug("Ignoring message with no value (Kafka log compaction message). Message key:\n{}", kafkaMessageKey);
            return;
        }

        MessageContainer messageContainer = messageContainerFactory.create(topic, kafkaMessageKey, kafkaMessageValue);

        List<String> typeNames = metadataHandler.getTypesForMessage(messageContainer);
        for (String typeName : typeNames) handleType(typeName, messageContainer);
    }

    private void handleType(String typeName, MessageContainer messageContainer) {
        SpaceTypeDescriptor spaceTypeDescriptor = this.space.getTypeManager().getTypeDescriptor(typeName);
        if (spaceTypeDescriptor == null)
            throw new DataException("Type descriptor for '{}' not found in space.", typeName);

        Pair<CdcOperation, String> operationAndPropSelectorTemplate = cdcOperationResolver.resolve(messageContainer);
        CdcOperation cdcOperation = operationAndPropSelectorTemplate.getA();
        logger.debug("Will perform CDC operation '{}'", cdcOperation);

        messageContainer.setPropertySelectorTemplate(operationAndPropSelectorTemplate.getB());
        switch (cdcOperation) {
            case INSERT:
                handleInsert(spaceTypeDescriptor, messageContainer);
                break;

            case UPDATE:
                handleUpdate(spaceTypeDescriptor, messageContainer);
                break;

            case DELETE:
                handleDelete(spaceTypeDescriptor, messageContainer);
                break;

            default:
                throw new CdcException("Unknown CDC operation '{}'", cdcOperation);
        }
    }

    private void handleInsert(SpaceTypeDescriptor typeDescriptor, MessageContainer messageContainer) {
        SpaceDocument spaceDocument = new SpaceDocument(typeDescriptor.getTypeName());

        propertiesSetter.setProperties(typeDescriptor, messageContainer, spaceDocument::setProperty, true, false);
        logger.debug("SpaceDocument is: " + spaceDocument);
        if (cdcOperationResolver.getIfExists().equals(DataPipelineConfig.IfExists.override)) {
            space.write(spaceDocument);
            logger.debug("Wrote object '{}' with ID={} to space '{}'",
                    spaceDocument.getTypeName(), spaceDocument.getProperty(typeDescriptor.getIdPropertyName()), space.getSpace().getName());
        } else {
            Object spaceIdValue = getSpaceIdValue(typeDescriptor, messageContainer);
            IdQuery<Object> idQuery = new IdQuery<>(typeDescriptor.getTypeName(), spaceIdValue);
            int count = space.count(idQuery);

            if (count > 0) {
                logger.debug("Handling INSERT. Object '{}' with ID={} already exists in space '{}'.",
                        spaceDocument.getTypeName(), spaceDocument.getProperty(typeDescriptor.getIdPropertyName()), space.getSpace().getName());
                logger.debug("IfExists is set to '{}'", cdcOperationResolver.getIfExists());
                switch (cdcOperationResolver.getIfExists()) {
                    case update:
                        handleUpdate(typeDescriptor, messageContainer);
                        break;
                    case skip:
                        logger.debug("Skipping message.");
                        break;
                    case terminate:
                        throw new DataException("Got INSERT operation for object '{}' with ID={} in space '{}'. Object already exists. Terminating.",
                                typeDescriptor.getTypeName(), spaceIdValue, space.getSpace().getName());
                }
            } else {
                space.write(spaceDocument);
                logger.debug("Wrote object '{}' with ID={} to space '{}'",
                        spaceDocument.getTypeName(), spaceDocument.getProperty(typeDescriptor.getIdPropertyName()), space.getSpace().getName());

            }
        }
    }

    private void handleUpdate(SpaceTypeDescriptor typeDescriptor, MessageContainer messageContainer) {
        Object spaceIdValue = getSpaceIdValue(typeDescriptor, messageContainer);

        IdQuery<Object> idQuery = new IdQuery<>(typeDescriptor.getTypeName(), spaceIdValue);

        ChangeSet changeSet = new ChangeSet();
        PropertiesSetter.SetNameValue func = (name, value) -> changeSet.set(name, (Serializable) value);
        propertiesSetter.setProperties(typeDescriptor, messageContainer, func, false, true);
        ChangeResult<Object> change = this.space.change(idQuery, changeSet);
        int numberOfChangedEntries = change.getNumberOfChangedEntries();
        if (numberOfChangedEntries > 0) {
            logger.debug("Changed object '{}' with ID={} in space '{}'",
                    typeDescriptor.getTypeName(), spaceIdValue, space.getSpace().getName());
        } else {
            logger.debug("numberOfChangedEntries=0 after trying to change object '{}' with ID={} in space '{}'.",
                    typeDescriptor.getTypeName(), spaceIdValue, space.getSpace().getName());
            logger.debug("IfNotExists is set to '{}'", cdcOperationResolver.getIfNotExists());
            switch (cdcOperationResolver.getIfNotExists()) {
                case skip:
                    logger.debug("Skipping the message");
                    break;
                case terminate:
                    throw new DataException("Got UPDATE operation for object '{}' with ID={} in space '{}'. Object does not exist. Terminating.",
                            typeDescriptor.getTypeName(), spaceIdValue, space.getSpace().getName());
                default:
                    logger.debug("Falling back to INSERT operation.");
                    handleInsert(typeDescriptor, messageContainer);
            }
        }
    }

    private void handleDelete(SpaceTypeDescriptor typeDescriptor, MessageContainer messageContainer) {
        Object spaceIdValue = getSpaceIdValue(typeDescriptor, messageContainer);
        Object[] ids = {spaceIdValue};

        IdsQuery<Object> query = new IdsQuery<>(typeDescriptor.getTypeName(), ids);
        this.space.clear(query);

        logger.debug("Deleted object '{}' with ID={} in space '{}'",
                typeDescriptor.getTypeName(), spaceIdValue, space.getSpace().getName());
    }

    private Object getSpaceIdValue(SpaceTypeDescriptor typeDescriptor, MessageContainer messageContainer) {
        String idPropertyName = typeDescriptor.getIdPropertyName();

        DataPipelineConfig.Property property = metadataHandler.getPropertyDefinition(typeDescriptor.getTypeName(), idPropertyName);
        String selectorForIdProp = property.getSelector();
        if (messageContainer.getPropertySelectorTemplate() != null)
            // TODO: 29/12/2021 DRY
            selectorForIdProp = String.format(messageContainer.getPropertySelectorTemplate(), selectorForIdProp);

        Parser parser = messageContainer.getValueParser();
        if (!parser.containsValue(selectorForIdProp))
            throw new DataException("Operation failed because message does not contain ID value at path '{}'", selectorForIdProp);

        Object value = parser.getValue(selectorForIdProp);

        Object spaceValueForProp = valueToSpaceTypeConverter.convert(value, property);

        return spaceValueForProp;
    }
}
