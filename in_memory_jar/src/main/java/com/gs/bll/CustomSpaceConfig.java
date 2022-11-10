package com.gs.bll;

import org.openspaces.core.config.annotation.EmbeddedSpaceBeansConfig;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

@PropertySource("classpath:gs-service-config.yaml")
public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {
    Logger logger = LoggerFactory.getLogger(CustomSpaceConfig.class);
    @Value("${space.propertyFilePath}")
    private String spacePropertyFilePath;

    @Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
        super.configure(factoryBean);
        Properties spaceProperties = new Properties();
        logger.info("################### configure() ###########################");
        logger.info("spacePropertyFilePath : " + spacePropertyFilePath);
        logger.info("spaceName" + getSpaceName());
        boolean isSpacePropertyFileExist = checkIsFileExist(spacePropertyFilePath);
        logger.info("isSpacePropertyFileExist : " + isSpacePropertyFileExist);

        // load space properties file passed as param, if exist load properties
        if (isSpacePropertyFileExist) {
            spaceProperties = readPropertyFileAndSetValue(spaceProperties);
            for (Object key : spaceProperties.keySet()) {
                logger.info("Key : " + key + " Value : " + spaceProperties.getProperty(key.toString()));
            }
            factoryBean.setProperties(spaceProperties);
        }
    }

    private Properties readPropertyFileAndSetValue(Properties properties) {
        logger.info("readPropertyFileAndSetValue");
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            logger.info("spacePropertyFilePath : " + spacePropertyFilePath);
            InputStream in = classLoader.getResourceAsStream(spacePropertyFilePath);
            if (in == null)
                logger.info("in==null");
            in = new FileInputStream(spacePropertyFilePath);
            logger.info("in completed :");
            properties.load(in);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    private boolean checkIsFileExist(String filepath) {
        BufferedReader bufferedReader = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(filepath);
            if (in == null) {
                bufferedReader = new BufferedReader(new FileReader(filepath));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return false;
    }
}
