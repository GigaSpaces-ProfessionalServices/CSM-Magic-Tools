package com.gigaspaces.objectManagement.utils;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Properties;

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
            logger.error(message+"->"+e.getMessage());
            throw new RuntimeException(message);
        }

        return properties;
    }

    public static void addSpaceId(String spaceId, String spaceIdType, SpaceTypeDescriptorBuilder builder) throws ClassNotFoundException {

        if (spaceId != null) {
            builder.idProperty(spaceId);
            builder.addFixedProperty(spaceId, Class.forName(spaceIdType));
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
    public static String getTierCriteriaConfig(String typeName,String strTierCriteriaFile){
        logger.info("Entering into -> getTierCriteriaConfig");
        logger.info("strTierCriteriaFile -> "+strTierCriteriaFile);
        try {
            if (strTierCriteriaFile != null && strTierCriteriaFile.trim() != "") {
                File tierCriteriaFile = new File(strTierCriteriaFile);
                if (tierCriteriaFile.exists()) {
                    logger.info("Tier Criteria config file exists");
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(tierCriteriaFile));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if(line!=null && line.indexOf(typeName)<0) continue;
                        logger.info("Tier criteria file line -> "+line);
                        String[] lineContents = line.split("\t");
                        if(lineContents!=null && lineContents.length > 2) {
                            String criteriaClass = lineContents[1];
                            logger.info("criteriaClass -> "+criteriaClass);
                            String criteria = lineContents[2];
                            logger.info("criteria -> "+criteria);

                            if (criteriaClass != null && criteriaClass.trim().equalsIgnoreCase(typeName)) {
                                if (criteria != null && criteria.trim() != "") {
                                    return criteria;
                                }
                            } else {
                                logger.info("Tier configuration for '" + criteriaClass + "' is not found");
                            }
                        } else{
                            logger.info("Tier criteria for '"+typeName+"' is not defined");
                        }
                    }
                } else {
                    logger.info("Tier Criteria file '" + strTierCriteriaFile + "' does not exists");
                }
            } else {
                logger.info("Tier Criteria file path is not configured");
            }
            logger.info("Exiting from -> getTierCriteriaConfig");
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in getTierCriteriaConfig -> "+e.toString());
        }
        return null;
    }
    public static SpaceTypeDescriptorBuilder setTierCriteria(String typeName, SpaceTypeDescriptorBuilder builder, String strTierCriteriaFile){
        logger.info("Entering into -> setTierCriteria");
        logger.info("strTierCriteriaFile -> "+strTierCriteriaFile);
        try {
            String criteria = getTierCriteriaConfig(typeName,strTierCriteriaFile);
            if(criteria!=null && criteria.trim()!=""){
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(typeName)
                        .setCriteria(criteria));
            }

            logger.info("Exiting from -> setTierCriteria");
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in setTierCriteria -> "+e.toString());
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

    public static Admin getAdmin(String lookupLocator, String lookupGroup, String odsxProfile, String username, String password){
        logger.info("Entering into -> getAdmin");
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.addLocator(lookupLocator);
        logger.info("Entering into -> LOOKUP_LOCATOR->"+lookupLocator);
        logger.info("Entering into -> LOOKUP_GROUP->"+lookupGroup);

        logger.info("ODSX profile ->"+odsxProfile);

        if(lookupGroup!=null && lookupGroup!=""){
            adminFactory.addGroups(lookupGroup);
        }
        if (odsxProfile!=null && odsxProfile!="" && odsxProfile.equalsIgnoreCase("security")) {
            logger.info("setting credentials to grid manager admin");
            logger.info("GS_USERNAME -> "+username);
            logger.info("GS_PASSWORD -> "+password);
            adminFactory.credentials(username, password);
        }
        Admin admin =adminFactory.createAdmin();

        logger.info("Exiting from -> getAdmin()");
        return admin;
    }
}
