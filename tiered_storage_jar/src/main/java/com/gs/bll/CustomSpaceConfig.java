package com.gs.bll;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageConfig;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import org.openspaces.core.config.annotation.EmbeddedSpaceBeansConfig;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.openspaces.core.space.TieredStorageCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

import java.io.BufferedReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.gs.bll.CommonUtil.checkIsFileExist;
import static com.gs.bll.CommonUtil.readPropertiesFile;
import static com.gs.bll.CommonUtil.readPropertyFileAndSetValue;

@PropertySource("classpath:gs-service-config.yaml")
public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {
    private Logger logger = LoggerFactory.getLogger(CustomSpaceConfig.class);
    @Value("${tieredCriteriaConfig.filePath}")
    private String tieredConfigFilePath;

    @Value("${space.propertyFilePath}")
    private String spacePropertyFilePath;

    @Value("${tieredCriteriaConfig.dirtyBitFile}")
    private String tsdirtyBitFile;

    @Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
        super.configure(factoryBean);
        Properties spaceProperties = new Properties();
        // load space properties file passed as param, if exist load properties
        boolean isSpacePropertyFileExist = checkIsFileExist(spacePropertyFilePath);
        logger.info("spacePropertyFilePath : " + spacePropertyFilePath + ", spaceName : " + getSpaceName() + ", isSpacePropertyFileExist : " + isSpacePropertyFileExist);
        // read value of dirty file bit. If it is deployed 1st time than will be true and need to skip the tiered criteria file. After deployment, it will be false
        Map<String, String> tsdirtyBitFileMap = readPropertiesFile(tsdirtyBitFile);
        boolean tsFileFlag = false;
        logger.info("tsdirtyBitFileMap " + tsdirtyBitFileMap);
        if (tsdirtyBitFileMap.containsKey("firstTimedeployment")) {
            tsFileFlag = tsdirtyBitFileMap.get("firstTimedeployment").equals("true");
            logger.info("firstTimedeployment flag " + tsFileFlag);
        }
        if (isSpacePropertyFileExist) {
            spaceProperties = readPropertyFileAndSetValue(spaceProperties, spacePropertyFilePath);
            for (Object key : spaceProperties.keySet()) {
                logger.info("Key : " + key + " Value : " + spaceProperties.getProperty(key.toString()));
            }
            //logger.info("cluster-config.groups.group.repl-policy.recovery-chunk-size :"+spaceProperties.get("cluster-config.groups.group.repl-policy.recovery-chunk-size"));
            factoryBean.setProperties(spaceProperties);
        }
        logger.info("tieredConfigFilePath : " + tieredConfigFilePath);
        TieredStorageConfig tieredStorageConfig = new TieredStorageConfig();
        // check if tsFileFlag false and tiered criteria file exist then set all the criteria
        if (!tsFileFlag && !tieredConfigFilePath.equals("")) {
            Map<String, TieredStorageTableConfig> tables = new HashMap<>();
            boolean isTieredConfigFileExist = checkIsFileExist(tieredConfigFilePath);
            logger.info("tieredConfigFilePath: " + tieredConfigFilePath + ", isTieredConfigFileExist " + isTieredConfigFileExist);
            if (isTieredConfigFileExist) {
                String lineJustFetched = null;
                String[] wordsArray;
                try {
                    BufferedReader bufferedReader = CommonUtil.getFileBufferedReader(tieredConfigFilePath);
                    while (true) {
                        lineJustFetched = bufferedReader.readLine();
                        if (lineJustFetched == null)
                            break;
                        else {
                            wordsArray = lineJustFetched.split("	");
                            if (wordsArray[0].equalsIgnoreCase("C")) {
                                logger.info("Catagory :" + wordsArray[0] + " DataType :" + wordsArray[1] + " Property :" + wordsArray[2]);
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria(wordsArray[2]));
                            }
                            if (wordsArray[0].equalsIgnoreCase("T")) {
                                logger.info("Time :");
                                logger.info(wordsArray[1] + " :: " + wordsArray[2] + " : " + wordsArray[3]);
                                Duration duration = Duration.parse(wordsArray[3]);
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(duration));
                            }
                            if (wordsArray[0].equalsIgnoreCase("A")) {
                                logger.info(wordsArray[1] + "ALL ");
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria("all"));
                            }
                            if (wordsArray[0].equalsIgnoreCase("R")) {
                                logger.info(wordsArray[1] + "Transient :: ");
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setTransient(true));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e.toString());
                }
            }
            logger.info("MAP :" + tables);
        } else {
            logger.info("Skipping Tiered Storage file configuration.");
        }
        factoryBean.setCachePolicy(new TieredStorageCachePolicy(tieredStorageConfig));
    }
}
