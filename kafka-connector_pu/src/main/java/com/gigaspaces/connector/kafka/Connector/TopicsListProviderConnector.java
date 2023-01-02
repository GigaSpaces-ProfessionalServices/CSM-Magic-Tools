package com.gigaspaces.connector.kafka.Connector;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class TopicsListProviderConnector {

    private static final Logger logger = LoggerFactory.getLogger(TopicsListProviderConnector.class);

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    private HashSet<String> topics = new HashSet<>();

    @PostConstruct
    public void initialize() {
        
        List<DataPipelineConfig.SpaceType> spaceTypes = dataPipelineConfig.getSpaceTypes();

        if (spaceTypes == null)
            throw new ConfigurationException("No 'spaceTypes' key found in data pipeline configuration.");

        for (DataPipelineConfig.SpaceType spaceType : spaceTypes) {
            if (spaceType.getDataSource() == null)
                throw new ConfigurationException("No 'dataSource' key found under space type definition '{}'", spaceType.getName());
            topics.add(spaceType.getDataSource().getTopic());
        }

        logger.info("Initialization finished. Topics list: {}", String.join(",", topics));
    }

    public String[] getAll() {
        return topics.toArray(new String[0]);
    }
}
