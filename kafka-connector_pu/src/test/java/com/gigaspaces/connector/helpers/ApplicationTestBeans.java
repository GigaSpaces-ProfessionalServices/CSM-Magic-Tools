package com.gigaspaces.connector.helpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gigaspaces.connector.cdc.CdcOperationResolver;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.data.*;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.connector.plugins.data.ParserFactory;
import com.gigaspaces.connector.plugins.metadata.MetadataProviderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.gigaspaces.internal.io.BootIOUtils.getResourcePath;

@Configuration
@EnableConfigurationProperties
public class ApplicationTestBeans {
    @Bean
    @Profile(Consts.LEARNING_MODE)
    MetadataProviderConfig learningModeConfig() {
        return new MetadataProviderConfig();
    }

    @Bean
    DataHandler messageProcessor() {
        return new DataHandler();
    }

    @Bean
    @Profile(Consts.LEARNING_MODE)
    DataPipelineConfigGenerator dataPipelineConfigGenerator() {
        return new DataPipelineConfigGenerator();
    }

    @Bean
    MetadataHandler spaceTypesManager() {
        return new MetadataHandler();
    }

    @Bean
    ParserFactory parserFactory() {
        return new ParserFactory();
    }

    @Bean
    MetadataProviderFactory metadataProviderFactory() {
        return new MetadataProviderFactory();
    }

    @Bean
    CdcOperationResolver cdcOperationResolver() {
        return new CdcOperationResolver();
    }

    @Bean
    DataPipelineConfig dataPipelineConfig() {
        DataPipelineConfig dataPipelineConfig = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            dataPipelineConfig = mapper.readValue(new File(getApplicationYamlFilePath()), DataPipelineConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataPipelineConfig;
    }

    @Bean
    PropertiesSetter propertiesSetter() {
        return new PropertiesSetter();
    }

    @Bean
    MessageContainerFactory messageContainerFactory() {
        return new MessageContainerFactory();
    }

    @Bean
    ValueToSpaceTypeConverter valueToSpaceTypeConverter() {
        return new ValueToSpaceTypeConverter();
    }

    @Bean
    ConditionMatcher conditionMatcher() {
        return new ConditionMatcher();
    }

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public String getApplicationYamlFilePath() {
        String[] split = activeProfile.split(",");
        String mainTestProfile = split[0];
        Path resourcePath = null;
        try {
            resourcePath = getResourcePath(String.format("application-%s.yml", mainTestProfile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.valueOf(resourcePath);
    }

}
