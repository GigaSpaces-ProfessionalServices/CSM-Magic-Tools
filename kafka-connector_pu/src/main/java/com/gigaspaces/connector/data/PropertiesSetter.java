package com.gigaspaces.connector.data;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.connector.plugins.data.Parser;
import com.gigaspaces.document.DocumentProperties;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class PropertiesSetter {
    public static final Logger logger = LoggerFactory.getLogger(PropertiesSetter.class);

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private ValueToSpaceTypeConverter valueToSpaceTypeConverter;

    public void setProperties(SpaceTypeDescriptor spaceTypeDescriptor, MessageContainer messageContainer, SetNameValue func, boolean includingIdProp, boolean skipIfNotFound) {
        Parser parser = messageContainer.getValueParser();
        String selectorTemplate = messageContainer.getPropertySelectorTemplate();

        DataPipelineConfig.SpaceType spaceType = metadataHandler.getDefinitionForType(spaceTypeDescriptor.getTypeName());
        for (DataPipelineConfig.Property p : spaceType.getProperties()) {

            String name = p.getName();

            if (!includingIdProp && spaceTypeDescriptor.getIdPropertyName().equals(name))
                continue;

            String selector = p.getSelector();
            if (selectorTemplate != null) selector = String.format(selectorTemplate, selector);

            logger.debug("type={} name={} selector={}", spaceType.getName(), name, selector);

            if (p.getProperties() == null) {
                Object value;
                if (parser.containsValue(selector)) {
                    value = parser.getValue(selector);
                } else {
                    // TODO: 13/12/2021 UT for list as default value
                    // TODO: 04/01/2022 this will set null if default value is not set - is that correct?
                    if (!skipIfNotFound) func.set(name, p.getDefaultValue());
                    continue;
                }
                Object spaceValue = valueToSpaceTypeConverter.convert(value, p);
                func.set(name, spaceValue);
            } else {
                // nested properties
                // TODO: 21/11/2021 default values support here
                if (isList(p)) {
                    List<DocumentProperties> listOfDocumentProperties = objectPropsToListOfDocumentProps(p.getProperties(), parser, selector);
                    func.set(name, listOfDocumentProperties);
                } else {
                    DocumentProperties documentProperties = objectPropsToDocumentProps(p.getProperties(), parser, selector);
                    func.set(name, documentProperties);
                }
            }
        }
    }

    private DocumentProperties objectPropsToDocumentProps(List<DataPipelineConfig.Property> properties, Parser parser, String parentPath) {
        DocumentProperties documentProperties = new DocumentProperties();
        for (DataPipelineConfig.Property p : properties) {
            String name = p.getName();

            if (p.getProperties() == null) {
                Object spaceValue = getSpaceValueOrDefaultForProperty(p, parser, parentPath);
                documentProperties.setProperty(name, spaceValue);
            } else {
                if (isList(p)) {
                    List<DocumentProperties> listOfDocumentProperties = objectPropsToListOfDocumentProps(p.getProperties(), parser, parentPath);
                    documentProperties.setProperty(name, listOfDocumentProperties);
                } else {
                    DocumentProperties nestedProps = objectPropsToDocumentProps(p.getProperties(), parser, parentPath);
                    documentProperties.setProperty(name, nestedProps);
                }
            }
        }
        return documentProperties;
    }

    private List<DocumentProperties> objectPropsToListOfDocumentProps(List<DataPipelineConfig.Property> properties, Parser parser, String parentSelector) {
        int listSize = ((List) parser.getValue(parentSelector)).size();
        DocumentProperties[] documentPropertiesArray = new DocumentProperties[listSize];
        for (int i = 0; i < listSize; i++) {
            // TODO: 30/12/2021 this is JSON specific logic, needs to be in JsonParser
            // replace [*] with [i] (i as number) in parent selector, example '$.something[*]' -> '$.something[0]'
            String jsonPathIdx = "[" + String.valueOf(i) + "]";
            String currentParentPath = parentSelector.replaceFirst("\\[\\*\\]", jsonPathIdx);

            DocumentProperties documentProperties = objectPropsToDocumentProps(properties, parser, currentParentPath);
            documentPropertiesArray[i] = documentProperties;
        }

        return Arrays.asList(documentPropertiesArray);
    }

    private boolean isList(DataPipelineConfig.Property p) {
        return p.getAttributes() != null && p.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.list);
    }

    private Object getSpaceValueOrDefaultForProperty(DataPipelineConfig.Property property, Parser parser, String parentPath) {
        String selector = parentPath + property.getSelector();

        Object value;
        if (parser.containsValue(selector)) value = parser.getValue(selector);
        else value = property.getDefaultValue();

        Object spaceValueForProp = valueToSpaceTypeConverter.convert(value, property);
        return spaceValueForProp;
    }

    interface SetNameValue {
        void set(String name, Object value);
    }

}
