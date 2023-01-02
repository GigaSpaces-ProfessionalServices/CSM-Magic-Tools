package com.gigaspaces.connector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;

@Configuration
//@Profile(Consts.CONNECTOR_MODE)
public class DataPipelineConfigBeanCreator {

    @Value("${pipeline.config.location}")
    private String configLocation;

    @Bean
    public DataPipelineConfig dataPipelineConfig() {
        DataPipelineConfig dataPipelineConfig = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            dataPipelineConfig = mapper.readValue(new File(configLocation), DataPipelineConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConfigurationException("Failed to load data pipeline configuration file '{}'", configLocation);
        }
        return dataPipelineConfig;
    }
}
