package com.gigaspaces.connector.plugins.metadata;

import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;

import java.util.List;

public interface MetadataProvider {

    void putConfiguration(MetadataProviderConfig config);

    List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions();

    DataPipelineConfig.Cdc createCdcConfig();

    String getDataFormat();
}
