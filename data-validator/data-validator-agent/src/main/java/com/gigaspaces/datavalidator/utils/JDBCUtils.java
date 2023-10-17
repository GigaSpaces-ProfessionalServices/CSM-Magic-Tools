package com.gigaspaces.datavalidator.utils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
    private static Logger logger = Logger.getLogger(JDBCUtils.class.getName());
    
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

		switch (dataSourceType) {

		case "gigaspaces":
			Class.forName("com.j_spaces.jdbc.driver.GDriver");
			connectionString = "jdbc:gigaspaces:url:jini://" + dataSourceHostIp + ":" + dataSourcePort + "/*/"
					+ schemaName;
			if(gslookupGroup!=null && gslookupGroup.length()>0){
				connectionString += "?groups="+ gslookupGroup;
			}
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

			//
			if(authenticationScheme.equals("MSSQL")){
				//ds.setPortNumber(dataSourcePort);
				ds.setUser(username);
				ds.setPassword(password);
			}

			//

			logger.info("######## MS SQL Data Source: ########");
			logger.info("dataSourceHostIp: "+dataSourceHostIp);
			logger.info("integratedSecurity: "+integratedSecurity);
			logger.info("authenticationScheme: "+authenticationScheme);
			logger.info("schemaName: "+schemaName);
			if(authenticationScheme.equals("MSSQL")){
				logger.info("username: "+username);
				logger.info("password: "+password);
			}
			logger.info("############################");

			connection = ds.getConnection();


			/*Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
			connectionString = "jdbc:sqlserver://" + dataSourceHostIp + ":" + dataSourcePort + ";DatabaseName="+ schemaName + ";";

			if (integratedSecurity != null && integratedSecurity.trim().length() > 0) {
				connectionString = connectionString + "integratedSecurity=" + integratedSecurity + ";";
			}
			if (authenticationScheme != null && authenticationScheme.trim().length() > 0) {
				connectionString = connectionString + "authenticationScheme=" + authenticationScheme + ";";
			}
			if (properties != null && properties.trim().length() > 0) {
				connectionString = connectionString + properties;
			}*/

			break;
		}
		if(!dataSourceType.equals("ms-sql")) {
			logger.info("DataSource ConnectionString for " + measurement.getDataSourceType() + " :" + connectionString);
			connection = DriverManager.getConnection(connectionString, username, password);
		}

		return connection;
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