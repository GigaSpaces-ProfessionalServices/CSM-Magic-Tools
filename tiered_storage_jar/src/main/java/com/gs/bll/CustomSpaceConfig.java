package com.gs.bll;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageConfig;
import org.openspaces.core.config.annotation.EmbeddedSpaceBeansConfig;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.openspaces.core.space.TieredStorageCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.util.Properties;

import static com.gs.bll.CommonUtil.checkIsFileExist;
import static com.gs.bll.CommonUtil.readPropertyFileAndSetValue;

@PropertySource("classpath:gs-service-config.yaml")
public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {
    private Logger logger = LoggerFactory.getLogger(CustomSpaceConfig.class);
    @Value("${space.propertyFilePath}")
    private String spacePropertyFilePath;

    @Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
        super.configure(factoryBean);
        Properties spaceProperties = new Properties();
        // load space properties file passed as param, if exist load properties
        boolean isSpacePropertyFileExist = checkIsFileExist(spacePropertyFilePath);
        logger.info("spacePropertyFilePath : " + spacePropertyFilePath + ", spaceName : " + getSpaceName() + ", isSpacePropertyFileExist : " + isSpacePropertyFileExist);

        if (isSpacePropertyFileExist) {
            spaceProperties = readPropertyFileAndSetValue(spaceProperties, spacePropertyFilePath);
            for (Object key : spaceProperties.keySet()) {
                logger.info("Key : " + key + " Value : " + spaceProperties.getProperty(key.toString()));
            }
            factoryBean.setProperties(spaceProperties);
        }
        TieredStorageConfig tieredStorageConfig = new TieredStorageConfig();
        factoryBean.setCachePolicy(new TieredStorageCachePolicy(tieredStorageConfig));
    }
}
