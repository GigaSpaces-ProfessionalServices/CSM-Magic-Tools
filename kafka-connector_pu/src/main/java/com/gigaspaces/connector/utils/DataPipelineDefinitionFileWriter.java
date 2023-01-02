package com.gigaspaces.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Profile(Consts.LEARNING_MODE)
public class DataPipelineDefinitionFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(DataPipelineDefinitionFileWriter.class);

    @Value("${output.file.location:data-pipeline.yml}")
    private String outputFile;

    public void writeFile(DataPipelineConfig dataPipelineConfig) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            om.writeValue(new File(outputFile), dataPipelineConfig);
        } catch (IOException e) {
            logger.error("Failed to save pipeline definitions file " + outputFile, e);
            throw new RuntimeException();
        }

        logger.info("Saved pipeline definitions file {}", outputFile);
    }
}
