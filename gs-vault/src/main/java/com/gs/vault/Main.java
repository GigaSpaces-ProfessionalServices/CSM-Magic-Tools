package com.gs.vault;

import com.gs.vault.dao.VaultDao;
import com.gs.vault.model.Vault;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.gs.vault.PasswordVault.decryptPassword;
import static com.gs.vault.PasswordVault.encryptPassword;

public class Main {
    private static final Properties properties = new Properties();
    private static String configPath = null;
    private static String secretKey = "f1b4Fy2m6N2icOB5H0myUaA1FaY3fIxO";
    private static String algorithm = "AES";
    private static String keySizeDefault = "256";
    private static int keySize = 256;
    private static String cipherMode = "AES/CBC/PKCS5Padding";
    private String operation = null;
    private String propVal = null;
    private String propKey = null;
   // private Map<String,String> defaultCred = new HashMap<>();

    public static void main(String[] args) {
        try {
            Main main = new Main();
            main.processArgs(args);
            loadConfig();
            main.performOperation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        // Load properties file from resources folder within the JAR
        if (configPath == null) {
            try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("app.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                    setProperties();
                } else {
                    System.err.println("Unable to find the properties file.");
                }
            } catch (IOException e) {
                System.err.println("Error reading properties file: " + e.getMessage());
            }
        } else {
            try (FileInputStream fileInputStream = new FileInputStream(configPath)) {
                properties.load(fileInputStream);
                setProperties();
            } catch (IOException e) {
                System.err.println("Error reading properties file: " + e.getMessage());
            }
        }

    }

    private static void setProperties() {
        secretKey = properties.getProperty("app.secretKey", secretKey);
        algorithm = properties.getProperty("app.algorithm", algorithm);
        keySize = Integer.parseInt(properties.getProperty("app.keySize", keySizeDefault));
        cipherMode = properties.getProperty("app.cipherMode", cipherMode);
        if (System.getProperty("app.db.path")!=null && !"".equals(System.getProperty("app.db.path"))){
            VaultDao.DB_PATH = System.getProperty("app.db.path");
        } else {
            VaultDao.DB_PATH = properties.getProperty("app.db.path");
        }
        if (System.getProperty("app.db.name")!=null && !"".equals(System.getProperty("app.db.name"))){
            VaultDao.DB_PATH += System.getProperty("app.db.name");
        } else {
            VaultDao.DB_PATH += properties.getProperty("app.db.name");
        }
    }

    public static void printUsage() {
        System.out.println("GS Vault");
        System.out.println("The following arguments are used:");
        System.out.println("  --init");
        System.out.println("  --set property_name=<property_value>   Eg. --set manager_pass=gs-admin");
        System.out.println("  --get <property_name>");
        System.out.println("  --help. Displays this help message");
    }

    private void processArgs(String[] args) {
        int i = 0;
        while (i < args.length) {
            String s = args[i];
            String sUpper = s.toUpperCase();
            if (sUpper.startsWith("--help".toUpperCase())) {
                printUsage();
                System.exit(0);
            } else if (sUpper.startsWith("--init".toUpperCase())) {
                operation = "initialize";
            } else if (sUpper.startsWith("--get".toUpperCase())) {
                operation = "decrypt";
            } else if (sUpper.startsWith("--set".toUpperCase())) {
                operation = "encrypt";
            } else if (sUpper.startsWith("--config".toUpperCase())) {
                String[] sArray = s.split("=", 2);
                configPath = sArray[1];
            } else {
                String[] sArray = s.split("=", 2);
                propKey = sArray[0];
                if ("encrypt".equals(operation)) {
                    propVal = sArray[1];
                }
            }/* else {
                System.out.println("Please enter valid arguments.");
                printUsage();
                System.exit(-1);
            }*/
            i++;
        }
        if (!"initialize".equals(operation) && (operation == null || propKey == null)) {
            printUsage();
            System.exit(-1);
        }
    }

    private void performOperation() {
        if ("initialize".equals(operation)) {
            VaultDao vaultDao = new VaultDao();
            vaultDao.createTable();
            for (Map.Entry<String,String> entry:getDefaultCreds().entrySet()) {
                String encryptedPassword = encryptPassword(entry.getValue(), secretKey, algorithm, keySize, cipherMode);
                Vault vault = new Vault(entry.getKey(), encryptedPassword);
                vaultDao.storeProperty(vault);
            }
        } else if ("encrypt".equals(operation)) {
            String encryptedPassword = encryptPassword(propVal, secretKey, algorithm, keySize, cipherMode);
            Vault vault = new Vault(propKey, encryptedPassword);
            VaultDao vaultDao = new VaultDao();
            vaultDao.storeProperty(vault);
        } else if ("decrypt".equals(operation)) {
            VaultDao vaultDao = new VaultDao();
            propVal = vaultDao.getEncryptedValueByPropertyName(propKey);
           // System.out.println("propKey : "+propKey+",propVal : "+propVal);
            if(propVal != null) {
                String decryptedPassword = decryptPassword(propVal, secretKey, algorithm, keySize, cipherMode);
                System.out.println(decryptedPassword);
            }
        }
    }

    static Map<String,String> getDefaultCreds() {
        Map<String,String> defaultCred = new HashMap<>();
        defaultCred.put("storepass","123456");
        return defaultCred;
    }
}
