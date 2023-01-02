package com.gigaspaces.connector.plugins.metadata;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import org.springframework.stereotype.Component;

@Component
public class MetadataProviderFactory {

    public MetadataProvider create(MetadataProviderConfig config) {
        MetadataProvider metadataProvider;
        switch (config.getMethod()) {
            case IIDR:
                metadataProvider = new IidrDummyMetadataProvider();
                break;
            case basedondata:
                metadataProvider = getProviderBasedOnData(config);
                break;
            default:
                throw new ConfigurationException("Unexpected learning method value: '{}'", config.getMethod());
        }

        metadataProvider.putConfiguration(config);

        return metadataProvider;
    }

    private MetadataProvider getProviderBasedOnData(MetadataProviderConfig config) {
        if (config.getSchemalessJsonMetadataParser() != null) return new SchemalessJsonMetadataProvider();

        switch (config.getParser()) {
            case "HVR":
                return new HvrMetadataProvider();
            case "Debezium":
                return new DebeziumMetadataProvider();
            case "AdabasSoftwareAG":
                return new AdabaseSoftwareAGMetadataProvider();
            default:
                throw new ConfigurationException("Unexpected learning parser value: '{}'", config.getParser());
        }
    }
}
