package com.gigaspaces.connector.config;

import java.util.ArrayList;
import java.util.List;

public class SpaceTypeBuilder {

    private final String name;
    private String idPropName;
    private List<DataPipelineConfig.Property> properties;

    public SpaceTypeBuilder(String name) {
        this.name = name;
    }

    public void idProperty(String propName) {
        this.idPropName = propName;
    }

    public DataPipelineConfig.SpaceType create() {
        for (DataPipelineConfig.Property property : properties) {
            if (property.getName().equals(idPropName)) {
                ArrayList<DataPipelineConfig.Property.PropertyAttribute> attributes = new ArrayList<>();
                attributes.add(DataPipelineConfig.Property.PropertyAttribute.spaceid);
                property.setAttributes(attributes);
            }
        }

        DataPipelineConfig.SpaceType spaceType = new DataPipelineConfig.SpaceType();
        spaceType.setProperties(properties);

        spaceType.setName(name);

        return spaceType;
    }

    public void setProperties(List<DataPipelineConfig.Property> properties) {
        this.properties = properties;
    }
}
