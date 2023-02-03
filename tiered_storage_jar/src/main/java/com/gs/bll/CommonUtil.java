package com.gs.bll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

public class CommonUtil {
    private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

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

}
