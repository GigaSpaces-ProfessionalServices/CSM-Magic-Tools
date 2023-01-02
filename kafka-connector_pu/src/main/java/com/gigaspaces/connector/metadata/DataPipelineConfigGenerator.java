package com.gigaspaces.connector.metadata;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.plugins.metadata.MetadataProvider;
import com.gigaspaces.connector.plugins.metadata.MetadataProviderBasedOnData;
import com.gigaspaces.connector.plugins.metadata.MetadataProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile(Consts.LEARNING_MODE)
public class DataPipelineConfigGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DataPipelineConfigGenerator.class);

    @Autowired
    private MetadataProviderConfig metadataProviderConfig;

    @Autowired
    private MetadataProviderFactory metadataProviderFactory;

    private MetadataProvider metadataProvider;

    private DataPipelineConfig dataPipelineConfig;
    private final Set<String> learnedSpaceTypes = new HashSet<>();
    private MetadataProviderBasedOnData metadataProviderBasedOnData = null;

    @PostConstruct
    public void initialize() {
        dataPipelineConfig = new DataPipelineConfig();
        dataPipelineConfig.setSpaceTypes(new ArrayList<>());

        metadataProvider = metadataProviderFactory.create(metadataProviderConfig);
    }

    public void handleMessage(String topic, String key, String value) {
        MetadataProviderBasedOnData metadataProviderBasedOnData = getMetadataProviderBasedOnData();
        metadataProviderBasedOnData.putDataSample(topic, value);

        List<DataPipelineConfig.SpaceType> spaceTypes = metadataProvider.createSpaceTypeDefinitions();

        for (DataPipelineConfig.SpaceType st : spaceTypes) {
            if (learnedSpaceTypes.contains(st.getName())) {
                logger.debug("Type '{}' already processed. Skipping the message.", st.getName());
                continue;
            }

            if (st.getDataSource() == null) st.setDataSource(new DataPipelineConfig.DataSource());

            if (st.getDataSource().getTopic() == null)
                st.getDataSource().setTopic(topic);

            if (metadataProviderConfig.getIdColumnNames() != null) {
                for (DataPipelineConfig.Property p : st.getProperties()) {
                    if (metadataProviderConfig.getIdColumnNames().contains(p.getName())) {
                        if (p.getAttributes() == null) p.setAttributes(new ArrayList<>());
                        if (!p.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.spaceid))
                            p.getAttributes().add(DataPipelineConfig.Property.PropertyAttribute.spaceid);
                    }
                }
            }

            dataPipelineConfig.getSpaceTypes().add(st);
            learnedSpaceTypes.add(st.getName());
        }
    }

    private MetadataProviderBasedOnData getMetadataProviderBasedOnData() {
        if (metadataProviderBasedOnData != null) return metadataProviderBasedOnData;

        if (metadataProvider instanceof MetadataProviderBasedOnData) {
            metadataProviderBasedOnData = (MetadataProviderBasedOnData) metadataProvider;
        } else {
            throw new ConfigurationException("'{}' parser is not capable of learning based on data because it does not implement '{}' interface.",
                    metadataProviderConfig.getParser(), MetadataProviderBasedOnData.class.getName());
        }
        return metadataProviderBasedOnData;
    }

    public DataPipelineConfig getDataPipelineConfig() {
        dataPipelineConfig.setDataFormat(metadataProvider.getDataFormat());
        dataPipelineConfig.setCdc(metadataProvider.createCdcConfig());
        return dataPipelineConfig;
    }

}
