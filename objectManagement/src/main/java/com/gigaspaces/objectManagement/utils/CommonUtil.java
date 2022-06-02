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

public class CommonUtil {
    public static Properties readProperties(String propertiesFileName) throws FileNotFoundException {
        // Try to reload as resource from classpath
        InputStream inputStream = ObjectController.class.getClassLoader().getResourceAsStream(propertiesFileName + ".properties");
        BufferedReader br
                = new BufferedReader(new FileReader(propertiesFileName + ".properties"));
        System.out.println("Succeeded to load " + propertiesFileName + ".properties");

        if (inputStream == null) {
            String message = "Property file " + propertiesFileName + ".properties doesn't exist in classpath";
            System.out.println(message);
            // throw new RuntimeException(message);
        }

        Properties properties = new Properties();

        try {
            properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Failed to load properties from file [" + propertiesFileName + "]";
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

            if (spaceIndexType.equals("EQUAL"))
                builder.addPropertyIndex(indexField, SpaceIndexType.EQUAL);
            if (spaceIndexType.equals("ORDERED"))
                builder.addPropertyIndex(indexField, SpaceIndexType.ORDERED);
            if (spaceIndexType.equals("EQUAL_AND_ORDERED"))
                builder.addPropertyIndex(indexField, SpaceIndexType.EQUAL_AND_ORDERED);
        }
    }

    public static void dynamicPropertiesSupport(boolean synamicPropertiesSupported, SpaceTypeDescriptorBuilder builder) {
        builder.supportsDynamicProperties(synamicPropertiesSupported);
    }

    public static boolean registerType(SpaceTypeDescriptor typeDescriptor, GigaSpace gigaSpace) {

        try {

            gigaSpace.getTypeManager().registerTypeDescriptor(typeDescriptor);
            System.out.println("######## Successfully Register type 0.1 " + typeDescriptor.getTypeName() + " ########");

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean unregisterType(String type, GigaSpace gigaSpace) {

        try {

            gigaSpace.getTypeManager().unregisterTypeDescriptor(type);
            System.out.println("######## Successfully UnRegister type 0.1 " + type + " ########");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
