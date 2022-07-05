package com.gigaspaces.objectManagement.utils;

import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gigaspaces.objectManagement.controller.ObjectController;
import org.openspaces.core.GigaSpace;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class CommonUtil {
    private static final Logger logger = Logger.getLogger(CommonUtil.class.getName());

    public static Properties readProperties(String propertiesFileName) throws FileNotFoundException {
        logger.info("readProperties -> propertiesFileName=" + propertiesFileName);
        // Try to reload as resource from classpath
        InputStream inputStream = ObjectController.class.getClassLoader().getResourceAsStream(propertiesFileName + ".properties");
        BufferedReader br
                = new BufferedReader(new FileReader(propertiesFileName + ".properties"));
        logger.info("Succeeded to load " + propertiesFileName + ".properties");

        if (inputStream == null) {
            String message = "Property file " + propertiesFileName + ".properties doesn't exist in classpath";
            logger.info(message);
        }

        Properties properties = new Properties();

        try {
            properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Failed to load properties from file [" + propertiesFileName + "]";
            logger.severe(e.getMessage());
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
}
