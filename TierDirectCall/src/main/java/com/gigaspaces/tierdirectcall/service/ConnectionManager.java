package com.gigaspaces.tierdirectcall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionManager {
    private static final String JDBC_DRIVER = "org.sqlite.JDBC";
    private static final String USER = "gs";
    ;
    private static final String PASS = "gigaspaces";
    Path path;
    String dbName;
    Connection connection;
    private Logger logger = LoggerFactory.getLogger(ConnectionManager.class.getName());

    public ConnectionManager(Path workFolder, String spaceName, String fullMemberName) {
        this.path = workFolder.resolve("tiered-storage/" + spaceName);
        this.dbName = "sqlite_db" + "_" + fullMemberName;

    }

    public Connection connectToDB() throws ClassNotFoundException, SQLException {
        try {
            SQLiteConfig config = new SQLiteConfig();
            String dbUrl = "jdbc:sqlite:" + path + "/" + dbName;
            Class.forName(JDBC_DRIVER);
            Properties properties = config.toProperties();
            properties.setProperty("user", USER);
            properties.setProperty("password", PASS);
            connection = DriverManager.getConnection(dbUrl, properties);
            if (logger.isTraceEnabled()) {
                logger.trace("Successfully created connection to db {} in path {}", dbName, path);
            }
            return connection;
        } catch (Throwable t) {
            logger.error("Failed to create connection to db {} in path {}", dbName, path);
            return null;
        }
    }

    public void shutDown() {
        try {
            if (connection != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Trying close connection to db {}", dbName);
                }
                connection.close();
                if (logger.isTraceEnabled()) {
                    logger.trace("Successfully close connection to db {} in path {}", dbName, path);
                }
            }

        } catch (Throwable e) {
            logger.error("Failed to close connection to db {} in path {}", dbName, path);
        }
    }
}
