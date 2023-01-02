package com.gigaspaces.connector.plugins.metadata;

import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.config.SpaceTypeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IidrDummyMetadataProvider implements MetadataProvider {

    @Override
    public void putConfiguration(MetadataProviderConfig config) {

    }

    @Override
    public List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions() {
        SpaceTypeBuilder spaceTypeBuilder = new SpaceTypeBuilder("car");
        ArrayList<DataPipelineConfig.Property> properties = new ArrayList<>();

        DataPipelineConfig.Property p;
        p = createProperty("ID", Integer.class.getTypeName(), "$.ID");
        properties.add(p);
        p = createProperty("Make", String.class.getTypeName(), "$.Make");
        properties.add(p);
        p = createProperty("Model", String.class.getTypeName(), "$.Model");
        properties.add(p);
        p = createProperty("Year", Integer.class.getTypeName(), "$.Year");
        properties.add(p);

        spaceTypeBuilder.setProperties(properties);

        DataPipelineConfig.SpaceType spaceType = spaceTypeBuilder.create();

        return Collections.singletonList(spaceType);
    }

    private DataPipelineConfig.Property createProperty(String name, String typeName, String selector) {
        DataPipelineConfig.Property property = new DataPipelineConfig.Property();
        property.setName(name);
        property.setType(typeName);
        property.setSelector(selector);
        return property;
    }

    @Override
    public DataPipelineConfig.Cdc createCdcConfig() {
        return null;
    }

    @Override
    public String getDataFormat() {
        return "JSON";
    }
}
