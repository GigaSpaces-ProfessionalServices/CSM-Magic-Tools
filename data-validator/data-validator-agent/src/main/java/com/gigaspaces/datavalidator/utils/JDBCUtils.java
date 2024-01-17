package com.gigaspaces.datavalidator.utils;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.gigaspaces.datavalidator.model.MeasurementRequestModel;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class JDBCUtils {

    static List<String> aggregation_functions = new ArrayList<String>();
    static{
        aggregation_functions.add("avg");
        aggregation_functions.add("count");
        aggregation_functions.add("min");
        aggregation_functions.add("max");
        aggregation_functions.add("sum");
    }
    private static Logger logger = LoggerFactory.getLogger(JDBCUtils.class.getName());

    private static final Map<String, Connection> connectionPool = new HashMap<>();
    private static final Map<String, Admin> adminConnectionPool = new HashMap<>(); // Only for Gigaspaces Data source

    public static Connection getConnection(MeasurementRequestModel measurement)
			throws ReflectiveOperationException, ReflectiveOperationException, ClassNotFoundException, SQLException {
		
		String dataSourceType = measurement.getDataSourceType();
		String dataSourceHostIp = measurement.getDataSourceHostIp();
		String dataSourcePort = measurement.getDataSourcePort();
		String username = measurement.getUsername();
		String password = measurement.getPassword();
		String integratedSecurity = measurement.getIntegratedSecurity();
		String authenticationScheme = measurement.getAuthenticationScheme();
		String properties = measurement.getProperties();
		String schemaName = measurement.getSchemaName();
		String gslookupGroup = measurement.getGsLookupGroup();

		Connection connection = null;
		String connectionString = "";

        if(connectionPool.containsKey(measurement.getDataSourceIdentifierKey())){
            logger.debug("Found in Connection Pool for Key: "+measurement.getDataSourceIdentifierKey());
            connection = connectionPool.get(measurement.getDataSourceIdentifierKey());
            if(!connection.isClosed()){
                if(measurement.isKeepConnectionOpen()){
                    logger.debug("Connection is still Open & Keep connection open flag is TRUE for Key: "+measurement.getDataSourceIdentifierKey());
                    return connection;
                }else{
                    logger.debug("Connection is Open BUT Keep connection open flag is FALSE for Key: "+measurement.getDataSourceIdentifierKey());
                    connection.close();
                    connectionPool.remove(measurement.getDataSourceIdentifierKey());
                    connection=null;
                }
            }else{
                connection=null;
            }
        }

		switch (dataSourceType) {

		case "gigaspaces":
			Class.forName("com.j_spaces.jdbc.driver.GDriver");
			connectionString = "jdbc:gigaspaces:v3://" + dataSourceHostIp + ":" + dataSourcePort + "/"
					+ schemaName;
			break;

		case "mysql":
			Class.forName("com.mysql.cj.jdbc.Driver");
			connectionString = "jdbc:mysql://" + dataSourceHostIp + ":" + dataSourcePort + "/" + schemaName
					+ "?zeroDateTimeBehavior=CONVERT_TO_NULL";
			break;

		case "db2":
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			connectionString = "jdbc:db2://" + dataSourceHostIp + ":" + dataSourcePort + "/" + schemaName;
			break;

		case "oracle":
			Class.forName("oracle.jdbc.OracleDriver");
			connectionString = "jdbc:oracle:thin:@"+dataSourceHostIp+":"+dataSourcePort+":" + schemaName;
			break;

		case "ms-sql":
			SQLServerDataSource ds = new SQLServerDataSource();
			ds.setServerName(dataSourceHostIp);
			ds.setIntegratedSecurity(Boolean.parseBoolean(integratedSecurity));
			ds.setAuthenticationScheme(authenticationScheme);
			ds.setDatabaseName(schemaName);

			if(authenticationScheme.equals("MSSQL")){
				//ds.setPortNumber(dataSourcePort);
				ds.setUser(username);
				ds.setPassword(password);
			}
			connection = ds.getConnection();
			break;
		}
		if(!dataSourceType.equals("ms-sql")) {
			connection = DriverManager.getConnection(connectionString, username, password);
		}
        logger.debug("Created New Connection for Key: "+measurement.getDataSourceIdentifierKey());
        connectionPool.put(measurement.getDataSourceIdentifierKey(),connection);
		return connection;
	}

    public static Admin getAdminConnection(MeasurementRequestModel measurement){
        Admin admin = null;
        if(adminConnectionPool.containsKey(measurement.getDataSourceIdentifierKey())){
            logger.debug("Found in Admin Connection Pool for Key: "+measurement.getDataSourceIdentifierKey());
            admin = adminConnectionPool.get(measurement.getDataSourceIdentifierKey());
            if(!admin.isMonitoring()){
                if(measurement.isKeepConnectionOpen()){
                    logger.debug("Admin Connection is still Monitoring & Keep connection open flag is TRUE for Key: "+measurement.getDataSourceIdentifierKey());
                    return admin;
                }else{
                    logger.debug("Admin Connection is Monitoring BUT Keep connection open flag is FALSE for Key: "+measurement.getDataSourceIdentifierKey());
                    admin.close();
                    adminConnectionPool.remove(measurement.getDataSourceIdentifierKey());
                    admin=null;
                }
            }else{
                admin=null;
            }
        }
        if(measurement.getUsername()!=null
                && !measurement.getUsername().isEmpty()){
            admin = new AdminFactory()
                    //.addGroup(measurement.getGsLookupGroup())
                    .credentials(measurement.getUsername(), measurement.getPassword())
                    .addLocator(measurement.getDataSourceHostIp())
                    .createAdmin();
        }else{
            admin = new AdminFactory()
                    //.addGroup(measurement.getGsLookupGroup())
                    //.discoverUnmanagedSpaces()
                    .addLocator(measurement.getDataSourceHostIp())
                    .createAdmin();
        }
        return admin;
    }
    public static String buildQuery(String dataSource, String fieldName
            , String function, String tableName, long limitRecords, String whereCondition){
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        if(function != null && aggregation_functions.contains(function.toLowerCase())){
            if(fieldName != null && fieldName.equals("*")){
				query.append(function).append("(").append(fieldName).append(") ");
			}else{
				query.append(function).append("(A.").append(fieldName).append(") ");
			}
        }
        query.append(" FROM ");
        //use in future
        if(false && limitRecords != -1){
            switch(dataSource) {
                case "gigaspaces":
                    query.append(" (SELECT ").append(fieldName).append(" FROM ").append(tableName).append(" WHERE ROWNUM <= ").append(limitRecords).append(" ) A");
                    break;
                case "mysql":
                	 query.append(" (SELECT ").append(fieldName).append(" FROM ").append(tableName).append(" LIMIT ").append(limitRecords).append(" ) A");
                case "ms-sql":
                    query.append(" (SELECT ").append(fieldName).append(" FROM ").append(tableName).append(" LIMIT ").append(limitRecords).append(" ) A");
                    break;
                case "db2":
                    query.append(" (SELECT ").append(fieldName).append(" FROM ").append(tableName).append(" FETCH FIRST ").append(limitRecords).append(" ROWS ONLY").append(" ) A");
                    break;
				case "oracle":
					query.append(" (SELECT ").append(fieldName).append(" FROM ").append(tableName).append(" WHERE ROWNUM <= ").append(limitRecords).append(" ) A");
					break;
            }
        }else{
            query.append(tableName).append(" A");
            if(whereCondition != null && !whereCondition.trim().equals("")){
                query.append(" WHERE ").append(whereCondition);
            }
        }
        if(dataSource.equals("db2")){
            query.append(" with ur");
        }
        return query.toString();
    }
}