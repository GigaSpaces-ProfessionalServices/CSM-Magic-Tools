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
import org.springframework.security.core.parameters.P;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@PropertySource("classpath:gs-service-config.yaml")
public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {
    Logger logger = LoggerFactory.getLogger(CustomSpaceConfig.class);
    @Value("${tieredCriteriaConfig.filePath}")
    private String tieredConfigFilePath;

    @Value("${space.propertyFilePath}")
    private String spacePropertyFilePath;

    @Value("${tieredCriteriaConfig.dirtyBitFile}")
    private String tsdirtyBitFile;

    private String isTieredConfigFileExist;

    //@Value("${test}")
    //private String test;

    Map<String,String> readPropertiesFile(String filename) {
        BufferedReader br
                = null;
        Map<String, String> propertyFileContent = new HashMap<>();
        if (new File(filename).exists()) {
            try {
                br = new BufferedReader(new FileReader(filename));

                // Declaring a string variable
                String st;
                // Condition holds true till
                // there is character in a string
                while ((st = br.readLine()) != null) {
                    if (st == null || "".equals(st.trim()) || !st.contains("=")) {
                        continue;
                    }
                    String[] fileproperty = st.split("=");
                    propertyFileContent.put(fileproperty[0], fileproperty[1]);
                    // Print the string
                    System.out.println(st);
                }
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
        return propertyFileContent;
    }
    @Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
        super.configure(factoryBean);
        Properties spaceProperties = new Properties();
        logger.info("################### configure() ###########################");
        logger.info("spacePropertyFilePath : "+spacePropertyFilePath);
        logger.info("spaceName"+getSpaceName());
        boolean isSpacePropertyFileExist = checkIsFileExist(spacePropertyFilePath);
        logger.info("isSpacePropertyFileExist : "+isSpacePropertyFileExist);
        Map<String,String> tsdirtyBitFileMap = readPropertiesFile(tsdirtyBitFile);
        boolean tsFileFlag = false;
        logger.info("tsdirtyBitFileMap "+tsdirtyBitFileMap);
        if(tsdirtyBitFileMap.containsKey("firstTimedeployment")){
            tsFileFlag = tsdirtyBitFileMap.get("firstTimedeployment").equals("true");
            logger.info("firstTimedeployment flag "+tsFileFlag);
        }
        if(isSpacePropertyFileExist){
            spaceProperties = readPropertyFileAndSetValue(spaceProperties);
            for (Object key: spaceProperties.keySet()) {
                logger.info("Key : "+ key + " Value : " + spaceProperties.getProperty(key.toString()));
            }
            //logger.info("cluster-config.groups.group.repl-policy.recovery-chunk-size :"+spaceProperties.get("cluster-config.groups.group.repl-policy.recovery-chunk-size"));
            factoryBean.setProperties(spaceProperties);
        }
        logger.info("tieredConfigFilePath : "+tieredConfigFilePath);
        if(!tsFileFlag && !tieredConfigFilePath.equals("")) {
            System.out.println("################## " + tieredConfigFilePath + "###############");
            logger.info("################## tieredConfigFilePath #### here ##################" + tieredConfigFilePath);
            TieredStorageConfig tieredStorageConfig = new TieredStorageConfig();
            Map<String, TieredStorageTableConfig> tables = new HashMap<>();
            boolean isTieredConfigFileExist = checkIsFileExist(tieredConfigFilePath);
            System.out.println("isTieredConfigFileExist " + isTieredConfigFileExist);
            logger.info("isTieredConfigFileExist " + isTieredConfigFileExist);
            if (isTieredConfigFileExist) {
                System.out.println(" File Exist ::::: Starting Tiered StorageIMPL");
                logger.info(" File Exist ::::: Starting Tiered StorageIMPL");
                String lineJustFetched = null;
                String[] wordsArray;
                try {
                    BufferedReader bufferedReader = new CustomSpaceConfig().getFileBufferedReader(tieredConfigFilePath);
                    while (true) {
                        lineJustFetched = bufferedReader.readLine();
                        if (lineJustFetched == null)
                            break;
                        else {
                            wordsArray = lineJustFetched.split("	");
                            if (wordsArray[0].equalsIgnoreCase("C")) {
                                System.out.println("Catagory :" + wordsArray[0] + " DataType :" + wordsArray[1] + " Property :" + wordsArray[2]);
                                logger.info("Catagory :" + wordsArray[0] + " DataType :" + wordsArray[1] + " Property :" + wordsArray[2]);
                                //tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria(wordsArray[2]));
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria(wordsArray[2]));
                            }
                            if (wordsArray[0].equalsIgnoreCase("T")) {
                                System.out.println("Time :");
                                logger.info("Time :");
                                System.out.println(wordsArray[1] + " :: " + wordsArray[2] + " : " + wordsArray[3]);
                                logger.info(wordsArray[1] + " :: " + wordsArray[2] + " : " + wordsArray[3]);
                                Duration duration = Duration.parse(wordsArray[3]);
                                //tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(duration));
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(duration));
                            }
                            if (wordsArray[0].equalsIgnoreCase("A")) {
                                System.out.println(wordsArray[1] + "ALL ");
                                logger.info(wordsArray[1] + "ALL ");
                                //tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria("all"));
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria("all"));
                            }
                            if (wordsArray[0].equalsIgnoreCase("R")) {
                                System.out.println(wordsArray[1] + "Transient :: ");
                                logger.info(wordsArray[1] + "Transient :: ");
                                //tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTransient(true));
                                tieredStorageConfig.addTable(new TieredStorageTableConfig().setName(wordsArray[1]).setTransient(true));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e.toString());
                }
            }
            System.out.println("MAP :" + tables);
            logger.info("MAP :" + tables);
            //TieredStorageImpl Ends...
            //tieredStorageConfig.setTables(tables);

            //factoryBean.setTieredStorageConfig(tieredStorageConfig);  // Removed
            factoryBean.setCachePolicy(new TieredStorageCachePolicy(tieredStorageConfig));
        }else{
            logger.info("Skipping Tiered Storage file configuration.");
            TieredStorageConfig tieredStorageConfig = new TieredStorageConfig();
            factoryBean.setCachePolicy(new TieredStorageCachePolicy(tieredStorageConfig));
        }
    }

    private Properties readPropertyFileAndSetValue(Properties properties) {
        logger.info("readPropertyFileAndSetValue");
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            logger.info("spacePropertyFilePath : "+spacePropertyFilePath);
            InputStream in = classLoader.getResourceAsStream(spacePropertyFilePath);
            if(in==null)
                logger.info("in==null");
                in = new FileInputStream(spacePropertyFilePath);
            logger.info("in completed :");
            properties.load(in);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    private boolean checkIsFileExist(String tieredConfigFilePath) {
        BufferedReader bufferedReader=null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(tieredConfigFilePath);
            if (in != null) {
                return true;
            }
            if(in==null) {
                bufferedReader = new BufferedReader(new FileReader(tieredConfigFilePath));
                return true;
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }

    public BufferedReader getFileBufferedReader(String dataFile) throws Exception{
        BufferedReader bufferedReader=null;
        logger.info("dataFile : "+dataFile);
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(dataFile);
            if (in != null) {
                System.out.println("!=null");
                logger.info("!=null");
                setIsTieredConfigFileExist("true");
                System.out.println("isExit:"+getIsTieredConfigFileExist());
                logger.info("isExit:"+getIsTieredConfigFileExist());
                bufferedReader = new BufferedReader(new FileReader(classLoader.getResource(dataFile).getFile()));
            }
            if(in==null)
                logger.info("in==null");
                bufferedReader = new BufferedReader(new FileReader(dataFile));
        }catch(Exception e){
            e.printStackTrace();
        }
        return bufferedReader;
    }

    public String getIsTieredConfigFileExist() {
        return isTieredConfigFileExist;
    }
    public void setIsTieredConfigFileExist(String isTieredConfigFileExist) {
        this.isTieredConfigFileExist = isTieredConfigFileExist;
    }
}
