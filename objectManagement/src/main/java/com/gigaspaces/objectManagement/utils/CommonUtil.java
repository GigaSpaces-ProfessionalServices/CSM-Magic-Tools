package com.gigaspaces.objectManagement.utils;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gigaspaces.objectManagement.model.IndexDetail;
import com.gigaspaces.objectManagement.model.ObfuscatorEventContainerDetail;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.nio.file.Files.readAllBytes;

@Component
public class CommonUtil {
    private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    @Value("${odsx.profile}")
    private static String PROFILE;

    @Value("${gs.username}")
    private String GS_USERNAME;

    @Value("${gs.password}")
    private String GS_PASSWORD;

    @Value("${lookup.group}")
    private String LOOKUP_GROUP;

    @Value("${lookup.locator}")
    private String LOOKUP_LOCATOR;

    public static Properties readProperties(String propertiesFileName) throws FileNotFoundException {
        logger.info("readProperties -> propertiesFileName=" + propertiesFileName);
        // Try to reload as resource from classpath
        BufferedReader br
                = new BufferedReader(new FileReader(propertiesFileName + ".properties"));
        logger.info("Succeeded to load " + propertiesFileName + ".properties");

        Properties properties = new Properties();

        try {
            properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Failed to load properties from file [" + propertiesFileName + "]";
            logger.error(message + "->" + e.getMessage(), e);
            throw new RuntimeException(message);
        }

        return properties;
    }

    public static void addSpaceId(String spaceId, String spaceIdType, String broadcast, SpaceTypeDescriptorBuilder builder) throws ClassNotFoundException {
        // the spaceId is an id that we added. It can't be broadcast. It must be auto generated
        if (spaceId != null && spaceId.equals("internal_id")) {
            builder.idProperty(spaceId, true);
            builder.addFixedProperty(spaceId, Class.forName(spaceIdType));
            // the spaceId is a single field
        } else if (spaceId != null && !spaceId.equals("internal_id") && spaceId.indexOf(",") == -1) {
            builder.idProperty(spaceId);

            // the spaceId is compound
        } else if (spaceId != null && !spaceId.equals("internal_id") && spaceId.indexOf(",") != -1) {
            List<String> propertiesNames = new ArrayList<>(Arrays.asList(spaceId.split(",")));
            builder.idProperty(propertiesNames);
        }

        if (broadcast != null && broadcast.equals("true")) {
            builder.broadcast(true);
        }
    }

    public static void addRouting(String routingField, SpaceTypeDescriptorBuilder builder) {
        if (routingField != null) {
            builder.routingProperty(routingField);
        }
    }

    public static void addIndex(String indexField, String spaceIndexType, SpaceTypeDescriptorBuilder builder) {
        if (indexField != null & spaceIndexType != null) {
            String[] indexFieldArry = indexField.split(",");
            String[] spaceIndexTypeArry = spaceIndexType.split(",");
            if (indexFieldArry.length != spaceIndexTypeArry.length) {
                logger.info("SEVERE: Check the properties file - the number of indexes must be equals to the number of indexes type");
                return;
            }
            for (int idx = 0; idx < indexFieldArry.length; idx++) {
                if (spaceIndexTypeArry[idx].equals("EQUAL"))
                    builder.addPropertyIndex(indexFieldArry[idx], SpaceIndexType.EQUAL);
                if (spaceIndexTypeArry[idx].equals("ORDERED"))
                    builder.addPropertyIndex(indexFieldArry[idx], SpaceIndexType.ORDERED);
                if (spaceIndexTypeArry[idx].equals("EQUAL_AND_ORDERED"))
                    builder.addPropertyIndex(indexFieldArry[idx], SpaceIndexType.EQUAL_AND_ORDERED);
            }
        }
    }

    public static void dynamicPropertiesSupport(boolean synamicPropertiesSupported, SpaceTypeDescriptorBuilder builder) {
        builder.supportsDynamicProperties(synamicPropertiesSupported);
    }

    public static String[] getTierCriteriaConfig(String typeName, String strTierCriteriaFile) {
        logger.info("Entering into -> getTierCriteriaConfig");
        logger.info("strTierCriteriaFile -> " + strTierCriteriaFile);
        try {
            if (strTierCriteriaFile != null && strTierCriteriaFile.trim() != "") {
                File tierCriteriaFile = new File(strTierCriteriaFile);
                if (tierCriteriaFile.exists()) {
                    logger.info("Tier Criteria config file exists");
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(tierCriteriaFile));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line != null && line.indexOf(typeName) < 0) continue;
                        logger.info("Tier criteria file line -> " + line);
                        String[] lineContents = line.split("\t");
                        if (lineContents != null && lineContents.length > 2) {
                            String criteriaClass = lineContents[1];
                            logger.info("criteriaClass -> " + criteriaClass);
                            String criteria = lineContents[2];
                            logger.info("criteria -> " + criteria);

                            if (criteriaClass != null && criteriaClass.trim().equalsIgnoreCase(typeName)) {
                                if (criteria != null && criteria.trim() != "") {
                                    return lineContents;
                                }
                            } else {
                                logger.info("Tier configuration for '" + criteriaClass + "' is not found");
                            }
                        } else if (lineContents.length == 2 && lineContents[0].equals("A")) {
                            String criteriaClass = lineContents[1];
                            return lineContents;
                        } else if (lineContents.length == 2 && lineContents[0].equals("R")) {
                            String criteriaClass = lineContents[1];
                            return lineContents;
                        } else {
                            logger.info("Tier criteria for '" + typeName + "' is not defined");
                        }
                    }
                } else {
                    logger.info("Tier Criteria file '" + strTierCriteriaFile + "' does not exists");
                }
            } else {
                logger.info("Tier Criteria file path is not configured");
            }
            logger.info("Exiting from -> getTierCriteriaConfig");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getTierCriteriaConfig -> " + e.toString(), e);
        }
        return null;
    }

    public static SpaceTypeDescriptorBuilder setTierCriteria(String typeName, SpaceTypeDescriptorBuilder builder, String strTierCriteriaFile) {
        logger.info("Entering into -> setTierCriteria");
        logger.info("strTierCriteriaFile -> " + strTierCriteriaFile);
        try {
            String[] criteriaArray = getTierCriteriaConfig(typeName, strTierCriteriaFile);
            if (criteriaArray != null) {
                if (criteriaArray[0].equalsIgnoreCase("C")) {
                    logger.info("Category :" + criteriaArray[0] + " DataType :" + criteriaArray[1] + " Property :" + criteriaArray[2]);
                    builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                            .setName(criteriaArray[1])
                            .setCriteria(criteriaArray[2]));
                }
                if (criteriaArray[0].equalsIgnoreCase("T")) {
                    logger.info("Time Criteria:  " + criteriaArray[1] + " :: " + criteriaArray[2] + " : " + criteriaArray[3]);
                    Duration duration = Duration.parse(criteriaArray[3]);
                    builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                            .setName(criteriaArray[1])
                            .setTimeColumn(criteriaArray[2]).setPeriod(duration));
                }
                if (criteriaArray[0].equalsIgnoreCase("A")) {
                    logger.info(criteriaArray[1] + " ALL ");
                    builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                            .setName(criteriaArray[1])
                            .setCriteria("all"));
                }
                if (criteriaArray[0].equalsIgnoreCase("R")) {
                    logger.info(criteriaArray[1] + " Transient :: ");
                    builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                            .setName(criteriaArray[1])
                            .setTransient(true));
                }
            }

            logger.info("Exiting from -> setTierCriteria");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in setTierCriteria -> " + e.toString(), e);
        }
        return builder;
    }

    public static boolean registerType(SpaceTypeDescriptor typeDescriptor, GigaSpace gigaSpace) {

        try {


            gigaSpace.getTypeManager().registerTypeDescriptor(typeDescriptor);


            logger.info("######## Successfully Register type " + typeDescriptor.getTypeName() + " ########");

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean unregisterType(String type, GigaSpace gigaSpace) {

        try {

            gigaSpace.getTypeManager().unregisterTypeDescriptor(type);
            logger.info("######## Successfully UnRegister type " + type + " ########");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static void setCredentials(String lookupLocator, String lookupGroup, AdminFactory adminFactory, String appId, String safeId, String objectId) throws IOException, InterruptedException {
        String gsusername = "";
        String gspassword = "";
        String managerHost = lookupLocator.split(",")[0].split(":")[0];
        logger.info("lookupGroup : " + lookupGroup);
        logger.info("lookupLocator : " + lookupLocator);
        logger.info("managerHost : " + managerHost);

        /*Process p = Runtime.getRuntime().exec("ssh " + managerHost);
        PrintStream out = new PrintStream(p.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String safeusernameCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID=" + appId + " -p Query=\"Safe=" + safeId + ";Folder=;Object=" + objectId + ";\" -o PassProps.UserName";
        String safepassCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID=" + appId + " -p Query=\"Safe=" + safeId + ";Folder=;Object=" + objectId + ";\" -o Password";
        logger.info("safeusernameCmd :: " + safeusernameCmd);
        logger.info("safepassCmd :: " + safepassCmd);
        out.println(safeusernameCmd);
        while (in.ready()) {
            gsusername = in.readLine();
            logger.info(gsusername);
            break;
        }
        out.println(safepassCmd);
        while (in.ready()) {
            gspassword = in.readLine();
            logger.info(gspassword);
            break;
        }
        out.println("exit");
        p.waitFor();*/

        ProcessBuilder processBuilder = new ProcessBuilder("/usr/local/bin/odsx_vault_cred_details.sh");
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        // Start the process
        Process process = processBuilder.start();
        // Get the input stream of the process
        InputStream inputStream = process.getInputStream();
        // Read the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            gspassword=line;
        }
        // Wait for the process to finish
        int exitCode = process.waitFor();
        System.out.println("Command executed with exit code: " + exitCode);
        /*try {
            JSch jsch = new JSch();
            jsch.addIdentity("~/.ssh/id_rsa");

            Session session = null;

//            session = jsch.getSession("root", "172.31.46.143", 22);
            session = jsch.getSession(managerHost);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.info("connected >>>>>>>>>>>>>>>");
            String safeusernameCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID=" + appId + " -p Query=\"Safe=" + safeId + ";Folder=;Object=" + objectId + ";\" -o PassProps.UserName";
            String safepassCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID=" + appId + " -p Query=\"Safe=" + safeId + ";Folder=;Object=" + objectId + ";\" -o Password";
            logger.info("safeusernameCmd :: " + safeusernameCmd);
            logger.info("safepassCmd :: " + safepassCmd);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
//            channel.setCommand("pwd;ls");
            channel.setCommand(safeusernameCmd + ";" + safepassCmd);
            channel.connect();

            String msg = null;
            int counter = 0;
            while ((msg = in.readLine()) != null) {
                logger.info(msg);
                counter++;
                if (counter == 1) {
                    gsusername = msg;
                } else if (counter == 2) {
                    gspassword = msg;
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (JSchException e) {
            logger.error(e.getLocalizedMessage(), e);
        }*/
        if ("".equals(gsusername)) {
            gsusername = "gs-admin";
        }
        logger.info("GS_PASSWORD1 -> " + gspassword);
        if (gspassword.isEmpty()) {
            gspassword = "gs-admin";
        }
        adminFactory.credentials(gsusername, gspassword);
        logger.info("GS_USERNAME -> " + gsusername);
        logger.info("GS_PASSWORD2 -> " + gspassword);
    }

    public static Admin getAdmin(String lookupLocator, String lookupGroup, String odsxProfile, String username, String password, String appId, String safeId, String objectId) {
        logger.info("Entering into -> getAdmin");
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.addLocator(lookupLocator);
        logger.info("Entering into -> LOOKUP_LOCATOR->" + lookupLocator);
        logger.info("Entering into -> LOOKUP_GROUP->" + lookupGroup);

        logger.info("ODSX profile ->" + odsxProfile);

        if (lookupGroup != null && lookupGroup != "") {
            adminFactory.addGroups(lookupGroup);
        }
        if (lookupLocator != null && lookupLocator != "") {
            adminFactory.addLocators(lookupLocator);
        }
        if (odsxProfile != null && odsxProfile != "" && odsxProfile.equalsIgnoreCase("security")) {
            logger.info("setting credentials to grid manager admin");
            try {
                setCredentials(lookupLocator, lookupGroup, adminFactory, appId, safeId, objectId);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            } catch (InterruptedException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
            //  adminFactory.credentials(username, password);
        }
        Admin admin = adminFactory.createAdmin();

        logger.info("Exiting from -> getAdmin()");
        return admin;
    }

    public static String readDDLFromfile(String ddlFileName) throws IOException {
        String ddlText = new String(readAllBytes(Paths.get(ddlFileName)));
        return ddlText;
    }

    public static Properties readAdaptersProperties(String adaptersFilePath) {
        // Try to reload as resource from classpath
        File adaptersFile = new File(adaptersFilePath);
        //InputStream inputStream = CommonUtil.class.getClassLoader().getResourceAsStream("adapters.properties");
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(adaptersFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        logger.info("Succeeded to load adapters.properties");

        if (inputStream == null) {
            String message = "adapters.properties doesn't exist in classpath";
            logger.info(message);
            throw new RuntimeException(message);
        }

        Properties properties = new Properties();

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Failed to load properties from file adapters.properties";
            throw new RuntimeException(message);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return properties;
    }


    public static List<IndexDetail> getIndexDetails(String batchIndexFileStr) {
        logger.info("Entering into -> getIndexDetails");
        logger.info("batchIndexFile -> " + batchIndexFileStr);
        try {
            if (batchIndexFileStr != null && !batchIndexFileStr.trim().equals("")) {
                File batchIndexFile = new File(batchIndexFileStr);
                if (batchIndexFile.exists()) {
                    List<IndexDetail> indexDetails = new ArrayList<>();
                    logger.info("Batch Index exists");
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(batchIndexFile));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        logger.info("Batch Index file line -> " + line);
                        String[] lineContents = line.split("\t");
                        if (lineContents.length == 3) {
                            String tableName = lineContents[0];
                            String columnName = lineContents[1];
                            String indexType = lineContents[2];
                            IndexDetail indexDetail = new IndexDetail();
                            indexDetail.setIndexType(indexType);
                            indexDetail.setColumnName(columnName);
                            indexDetail.setTableName(tableName);
                            logger.info("indexDetail : " + indexDetail);
                            indexDetails.add(indexDetail);
                        } else {
                            logger.info("Batch Index for '" + line + "' is not defined");
                        }
                    }
                    return indexDetails;
                } else {
                    logger.info("Batch Index file '" + batchIndexFileStr + "' does not exists");
                }
            } else {
                logger.info("Batch Index file path is not configured");
            }
            logger.info("Exiting from -> getIndexDetails");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getIndexDetails -> " + e, e);
        }
        return new ArrayList<>();
    }

    public static List<ObfuscatorEventContainerDetail> getObfuscatorEventContainerDetail(String obfuscatorEventContainerDetailFileStr) {
        logger.info("Entering into -> getObfuscatorEventContainerDetail");
        logger.info("obfuscatorEventContainerDetailFile -> " + obfuscatorEventContainerDetailFileStr);
        try {
            if (obfuscatorEventContainerDetailFileStr != null && !obfuscatorEventContainerDetailFileStr.trim().isEmpty()) {
                File obfuscatorEventContainerDetailFile = new File(obfuscatorEventContainerDetailFileStr);
                if (obfuscatorEventContainerDetailFile.exists()) {
                    List<ObfuscatorEventContainerDetail> obfuscatorEventContainerDetails = new ArrayList<>();
                    logger.info("Obfuscator Event Container Detail File exists");
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(obfuscatorEventContainerDetailFile));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        logger.info("obfuscatorEventContainerDetail file line -> " + line);
                        if (line.startsWith("#")) {
                            logger.info("skipping commented line : " + line);
                            continue;
                        }
                        String[] lineContents = line.split("\t");
                        if (lineContents.length == 6) {

                            String typeName = lineContents[0];
                            String srcPropName = lineContents[1];
                            String destPropName = lineContents[2];
                            String obfuscatePropName = lineContents[3];
                            String obfuscationType = lineContents[4];
                            String spaceId = lineContents[5];

                            ObfuscatorEventContainerDetail obfuscatorEventContainerDetail = new ObfuscatorEventContainerDetail();
                            obfuscatorEventContainerDetail.setObfuscationType(obfuscationType);
                            obfuscatorEventContainerDetail.setObfuscatePropName(obfuscatePropName);
                            obfuscatorEventContainerDetail.setSrcPropName(srcPropName);
                            obfuscatorEventContainerDetail.setDestPropName(destPropName);
                            obfuscatorEventContainerDetail.setSpaceId(spaceId);
                            obfuscatorEventContainerDetail.setTypeName(typeName);

                            logger.info("indexDetail : " + obfuscatorEventContainerDetail);
                            obfuscatorEventContainerDetails.add(obfuscatorEventContainerDetail);
                        } else {
                            logger.info("obfuscatorEventContainerDetail for '" + line + "' is not defined");
                        }
                    }
                    return obfuscatorEventContainerDetails;
                } else {
                    logger.info("Obfuscator Event Container Detail file '" + obfuscatorEventContainerDetailFileStr + "' does not exists");
                }
            } else {
                logger.info("Obfuscator Event Container Detail file path is not configured");
            }
            logger.info("Exiting from -> getObfuscatorEventContainerDetail");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getObfuscatorEventContainerDetail -> " + e, e);
        }
        return new ArrayList<>();
    }
}
