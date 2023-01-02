package com.gigaspaces.connector.plugins.metadata;

public interface MetadataProviderBasedOnData {
    void putDataSample(String topic, Object data);
}
