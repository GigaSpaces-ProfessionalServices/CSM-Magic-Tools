package com.gs.bll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommonUtil {
    private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    static Map<String, String> readPropertiesFile(String filename) {
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
                    logger.info("st: " + st);
                }
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
        return propertyFileContent;
    }

    static Properties readPropertyFileAndSetValue(Properties properties, String spacePropertyFilePath) {
        logger.info("readPropertyFileAndSetValue");
        try {
            ClassLoader classLoader = CommonUtil.class.getClassLoader();
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

    static boolean checkIsFileExist(String tieredConfigFilePath) {
        BufferedReader bufferedReader = null;
        try {
            ClassLoader classLoader = CommonUtil.class.getClassLoader();
            InputStream in = classLoader.getResourceAsStream(tieredConfigFilePath);
            if (in != null) {
                return true;
            }
            if (in == null) {
                bufferedReader = new BufferedReader(new FileReader(tieredConfigFilePath));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }

    static BufferedReader getFileBufferedReader(String dataFile) throws Exception {
        BufferedReader bufferedReader = null;
        logger.info("dataFile : " + dataFile);
        try {
            ClassLoader classLoader = CommonUtil.class.getClassLoader();
            InputStream in = classLoader.getResourceAsStream(dataFile);
            if (in != null) {
                logger.info("!=null");
                bufferedReader = new BufferedReader(new FileReader(classLoader.getResource(dataFile).getFile()));
            }
            if (in == null)
                logger.info("in==null");
            bufferedReader = new BufferedReader(new FileReader(dataFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bufferedReader;
    }
}
