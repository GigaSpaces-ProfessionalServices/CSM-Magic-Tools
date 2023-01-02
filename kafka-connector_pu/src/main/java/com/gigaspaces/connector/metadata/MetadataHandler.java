package com.gigaspaces.connector.metadata;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.ConditionMatcher;
import com.gigaspaces.connector.data.MessageContainer;
import com.gigaspaces.document.DocumentProperties;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class MetadataHandler {

    public static final Logger logger = LoggerFactory.getLogger(MetadataHandler.class);
    @Autowired
    private GigaSpace space;
    @Autowired
    private ConditionMatcher conditionMatcher;
    private Map<String, DataPipelineConfig.SpaceType> typeToDefinition = new HashMap<>();
    private Map<String, List<String>> topicToTypes = new HashMap<>();
    private Map<String, Map<String, DataPipelineConfig.Property>> typePropNameToPropertyDefinition = new HashMap<>();

    public void createTypesInSpace(List<DataPipelineConfig.SpaceType> spaceTypeDefinitions) {
        GigaSpaceTypeManager typeManager = space.getTypeManager();
        if (spaceTypeDefinitions != null) {
            for (DataPipelineConfig.SpaceType spaceType : spaceTypeDefinitions) {
                registerType(typeManager, spaceType);

                typeToDefinition.put(spaceType.getName(), spaceType);

                String topic = spaceType.getDataSource().getTopic();
                if (!topicToTypes.containsKey(topic)) {
                    logger.debug("Added Type To Topic: {}",topic);
                    topicToTypes.put(topic, new ArrayList<>());
                }
                topicToTypes.get(topic).add(spaceType.getName());
            }
        } else {
            logger.warn("Types definitions not found");
        }
    }

    public DataPipelineConfig.SpaceType getDefinitionForType(String typeName) {
        return typeToDefinition.get(typeName);
    }

    public List<String> getTypesForMessage(MessageContainer messageContainer) {
        List<String> filteredList = new ArrayList<>();
        List<String> typesForTopic = topicToTypes.get(messageContainer.getTopic());
        logger.debug("typesForTopic : {},{}",messageContainer.getTopic(),typesForTopic);
        for (String spaceTypeName : typesForTopic) {
            DataPipelineConfig.SpaceType spaceType = typeToDefinition.get(spaceTypeName);
            List<DataPipelineConfig.Condition> conditions = spaceType.getDataSource().getConditions();
            if (conditions == null || conditionMatcher.anyConditionMatch(messageContainer, conditions))
                filteredList.add(spaceTypeName);
        }
        return filteredList;
    }

    public DataPipelineConfig.Property getPropertyDefinition(String typeName, String propName) {
        return typePropNameToPropertyDefinition.get(typeName).get(propName);
    }

    private void registerType(GigaSpaceTypeManager typeManager, DataPipelineConfig.SpaceType spaceType) {
        if (spaceType.getName() == null)
            throw new ConfigurationException("Missing 'name' key under one of space types definitions.");

        SpaceTypeDescriptorBuilder builder = new SpaceTypeDescriptorBuilder(spaceType.getName());
        builder.supportsDynamicProperties(false);
        List<DataPipelineConfig.Property> properties = spaceType.getProperties();

        if (properties == null)
            throw new ConfigurationException("Missing 'properties' key under space type definition '{}'", spaceType.getName());

        for (DataPipelineConfig.Property prop : properties) {
            boolean isList = prop.getAttributes() != null && prop.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.list);
            if (isList) {
                builder.addFixedProperty(prop.getName(), List.class);
            } else if (prop.getProperties() == null) {
                if (!typePropNameToPropertyDefinition.containsKey(spaceType.getName()))
                    typePropNameToPropertyDefinition.put(spaceType.getName(), new HashMap<>());
                typePropNameToPropertyDefinition.get(spaceType.getName()).put(prop.getName(), prop);

                builder.addFixedProperty(prop.getName(), prop.getType());
            } else {
                builder.addFixedProperty(prop.getName(), DocumentProperties.class);
            }
        }

        addIdProperty(builder, spaceType, properties);

        addRoutingProperty(builder, spaceType, properties);

        addIndexes(builder, spaceType);

        SpaceTypeDescriptor spaceTypeDescriptor = builder.create();
        typeManager.registerTypeDescriptor(spaceTypeDescriptor);
        logger.info("Registered space type '{}'", spaceTypeDescriptor.getTypeName());
    }

    private void addIndexes(SpaceTypeDescriptorBuilder builder, DataPipelineConfig.SpaceType spaceType) {
        List<DataPipelineConfig.Index> indexes = spaceType.getIndexes();
        if (indexes == null) return;

        for (DataPipelineConfig.Index idx : indexes) {
            if (idx.getProperties().size() == 1) {
                String idxProp = idx.getProperties().get(0);
                builder.addPropertyIndex(idxProp, idx.getType());
            } else {
                if (!idx.getType().equals(SpaceIndexType.EQUAL))
                    logger.warn("Check '{}' type definition. Only '{}' type is supported for compound index. '{}' value will be ignored.",
                            spaceType.getName(), SpaceIndexType.EQUAL, idx.getType());
                builder.addCompoundIndex(idx.getProperties().toArray(new String[0]), idx.getUnique());
            }
        }
    }

    private void addRoutingProperty(SpaceTypeDescriptorBuilder builder, DataPipelineConfig.SpaceType spaceType, List<DataPipelineConfig.Property> properties) {
        List<DataPipelineConfig.Property> routingKeyProps =
                properties.stream().filter(
                        p -> p.getAttributes() != null && p.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.routingkey)
                ).collect(Collectors.toList());

        if (routingKeyProps.size() == 0)
            logger.info("Routing property is not defined for type '{}'. Space ID will be used for routing.", spaceType.getName());

        if (routingKeyProps.size() > 1) throw new
                ConfigurationException("Multiple routing properties is not supported. Check type '{}'.", spaceType.getName());

        if (routingKeyProps.size() > 0)
            builder.routingProperty(routingKeyProps.get(0).getName());
    }

    private void addIdProperty(SpaceTypeDescriptorBuilder builder, DataPipelineConfig.SpaceType spaceType, List<DataPipelineConfig.Property> properties) {
        List<DataPipelineConfig.Property> idProps =
                properties.stream().filter(
                        p -> p.getAttributes() != null && p.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.spaceid)
                ).collect(Collectors.toList());

        if (idProps.size() == 0)
            throw new ConfigurationException("'{}' attribute must be set on one of the properties of '{}'.",
                    DataPipelineConfig.Property.PropertyAttribute.spaceid, spaceType.getName());

        if (idProps.size() > 1) throw new
                ConfigurationException("Multiple '{}' properties is not supported. Check type '{}'.",
                DataPipelineConfig.Property.PropertyAttribute.spaceid, spaceType.getName());

        builder.idProperty(idProps.get(0).getName());
    }
}
