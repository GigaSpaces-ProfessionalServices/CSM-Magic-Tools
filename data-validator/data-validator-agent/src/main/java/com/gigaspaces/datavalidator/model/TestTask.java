package com.gigaspaces.datavalidator.model;


import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.datavalidator.utils.JDBCUtils;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.GigaSpaceTypeManager;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TestTask  implements Serializable  {

	private static Logger logger = Logger.getLogger(TestTask.class.getName());

	private String uuid;
	private String result = null;
	private String errorSummary;
	private String query;
	private MeasurementRequestModel measurement;

	public TestTask() {	}

	public TestTask(String uuid, MeasurementRequestModel measurement) {
		this.uuid = uuid;
		this.measurement = measurement;
		this.result = "pending";
		this.errorSummary="";
		this.query="";
	}
	public String executeTask() {
		try {
				if (measurement != null) {
					//DataSource dataSource = measurement.getDataSource();
					logger.info("Executing task id: " + uuid);
					String whereCondition = measurement.getWhereCondition() != null ? measurement.getWhereCondition()
							: "";

					if(measurement.getDataSourceType().equals("gigaspaces")
							&& measurement.getFieldName().equals("*")
							&& whereCondition.equals("")){
						logger.info("### Admin API: Fetching record count ###");
						logger.info("Admin API - GS LookupGrp: "+measurement.getGsLookupGroup());
						logger.info("Admin API - Table name: "+measurement.getTableName());
						logger.info("Admin API - Connecting Space: "+measurement.getSchemaName());
						Admin admin = new AdminFactory()
								.addGroup(measurement.getGsLookupGroup())
								//.discoverUnmanagedSpaces()
								.createAdmin();
						String puName = measurement.getSchemaName().replace("space","service");
						ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(puName, 1, TimeUnit.MINUTES);
						if(processingUnit == null){
							this.errorSummary = "Processing unit not available with the name:"+puName;
							this.result = "Error";
							return this.result;
						}
						Space space = admin.getSpaces().waitFor(measurement.getSchemaName(),1, TimeUnit.MINUTES);
						logger.info("Admin API - Space: "+space.toString());
						for(SpaceInstance se: space.getInstances()){
							while(se.getMode().equals(SpaceMode.NONE)){
								Thread.sleep(400);
							}
						}
						Map<String, Integer> countPerClassName = space.getRuntimeDetails().getCountPerClassName();
						logger.info("Admin API - Table count: " + countPerClassName.get(measurement.getTableName()));


						Integer count = countPerClassName.get(measurement.getTableName());
						this.result = String.valueOf(count);
						this.query = "N/A";
						logger.info("### Admin API: Count="+this.result);
						return this.result;
					}
					String column_type=null;
					if(measurement.getDataSourceType().equals("gigaspaces")){
						logger.info("### NN Admin API: Fetching Column Type ###");
						logger.info("Admin API - GS LookupGrp: "+measurement.getGsLookupGroup());
						logger.info("Admin API - Table name: "+measurement.getTableName());
						logger.info("Admin API - Connecting Space: "+measurement.getSchemaName());

						GigaSpace gigaSpace = new GigaSpaceConfigurer(new SpaceProxyConfigurer(measurement.getSchemaName()).lookupGroups(measurement.getGsLookupGroup())).gigaSpace();
						GigaSpaceTypeManager gigaSpaceTypeManager = gigaSpace.getTypeManager();
						SpaceTypeDescriptor typeManager = gigaSpaceTypeManager.getTypeDescriptor(measurement.getTableName());
						SpacePropertyDescriptor fixedProperty = typeManager.getFixedProperty(measurement.getFieldName());
						column_type = fixedProperty.getTypeDisplayName();

						logger.info("Admin API - columnTypeSpace: " + column_type
								+" for column name: "+measurement.getFieldName()
								+" from table name: "+measurement.getTableName());
					}

					Connection conn = JDBCUtils.getConnection(measurement);
					Statement st = conn.createStatement();
					String query = JDBCUtils.buildQuery(measurement.getDataSourceType(), measurement.getFieldName(),
							measurement.getType(), measurement.getTableName(),
							Long.parseLong(measurement.getLimitRecords()), whereCondition);
					logger.info("query: " + query);
					ResultSet rs = st.executeQuery(query);

					if(!measurement.getDataSourceType().equals("gigaspaces")) {
						//Retrieving the ResultSetMetaData object
						ResultSetMetaData rsmd = rs.getMetaData();
						try {
							column_type = rsmd.getColumnClassName(1);
						} catch (Exception e) {
							logger.warning("Error in getting column data type so assuming string");
						}
					}
					String val = "";
					while (rs.next()) {
						logger.info("Column Type:     " + column_type);
						if(column_type!=null
								&& (column_type.equalsIgnoreCase("java.sql.Timestamp"))
						){
							val = String.valueOf(rs.getTimestamp(1).getTime());
						}else if(column_type.equalsIgnoreCase("java.sql.Date")) {
							val = String.valueOf(rs.getDate(1).getTime());
						}else if( column_type.equalsIgnoreCase("java.time.LocalDateTime")){
							String dateStr = rs.getString(1);
							String dateFormat = "yyyy-MM-dd HH:mm:ss";
							if(dateStr.indexOf("T")>0){
								dateFormat = "yyyy-MM-dd'T'HH:mm:ss";
							}
							logger.info("String val: " + dateStr);
							logger.info("dateFormat: " + dateStr);
							SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
							Date date = sdf.parse(dateStr);
							val= String.valueOf(date.getTime());
							logger.info("date final val: " + dateStr);

						}else{
							val = rs.getString(1);
						}
						logger.info("val:     " + val);
					}
					this.result = String.valueOf(val);
					this.query = query;
				} else {
					this.result = "FAIL";
					this.errorSummary="Measurement not available";
				}


		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e.getMessage());
			this.errorSummary = e.getMessage();
			this.result = "FAIL";
			return this.result;
		}
		return this.result;
	}

	

	public String getResult() {
		return (result == null) ? "pending" : result;
	}

	public void setResult(String result) {
		this.result = result;
	}


	public String toString() {
		return "[" + uuid + " " + result + "]";
	}

	public String getErrorSummary() {
		return errorSummary;
	}

	public void setErrorSummary(String errorSummary) {
		this.errorSummary = errorSummary;
	}

	public String getId() {
		return uuid;
	}

	public void setId(String uuid) {
		this.uuid = uuid;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}