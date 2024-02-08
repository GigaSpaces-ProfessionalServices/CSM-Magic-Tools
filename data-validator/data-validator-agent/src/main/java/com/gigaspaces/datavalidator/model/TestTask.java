package com.gigaspaces.datavalidator.model;


import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.common.task.MaxLocalDateTimeValueTaskNew;
import com.gigaspaces.common.task.MaxSqlDateValueTask;
import com.gigaspaces.common.task.MaxTimestampValueTask;
import com.gigaspaces.common.task.MaxValueTask;
import com.gigaspaces.datavalidator.utils.JDBCUtils;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import java.time.LocalDateTime;
import java.time.Month;
import org.openspaces.admin.Admin;
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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

public class    TestTask  implements Serializable  {

	private static Logger logger =LoggerFactory.getLogger(TestTask.class);

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
                    logger.debug("Executing task with id: " + uuid);
                    logger.debug("Executing task for table : " +measurement.getTableName() +"  And Column: "+measurement.getFieldName()
                    +"  On Data source: "+measurement.getDataSourceType());
					String whereCondition = measurement.getWhereCondition() != null ? measurement.getWhereCondition()
							: "";

                    String column_type=null;
					if(measurement.getDataSourceType().equals("gigaspaces")){
                        GigaSpace gigaSpace = new GigaSpaceConfigurer(new SpaceProxyConfigurer(measurement.getSchemaName())
                                .lookupGroups(measurement.getGsLookupGroup())
                                .lookupLocators(measurement.getDataSourceHostIp())).gigaSpace();
                        if(!measurement.getFieldName().equals("*")) {
                            GigaSpaceTypeManager gigaSpaceTypeManager = gigaSpace.getTypeManager();
                            SpaceTypeDescriptor typeManager = gigaSpaceTypeManager.getTypeDescriptor(
                                    measurement.getTableName());
                            SpacePropertyDescriptor fixedProperty = typeManager.getFixedProperty(
                                    measurement.getFieldName());
                            column_type = fixedProperty.getTypeDisplayName();
                        }
                        logger.debug("Gigaspaces Proxy - columnTypeInSpace: " + column_type
                                +" for column name: "+measurement.getFieldName()
                                +" from table name: "+measurement.getTableName());

                        if(measurement.getType().equals("count")
							&& whereCondition.equals("")){
                            logger.debug("### Admin API: Fetching record count ###");
                            logger.debug("Admin API - GS LookupGrp: "+measurement.getGsLookupGroup());
                            logger.debug("Admin API - Table name: "+measurement.getTableName());
                            logger.debug("Admin API - Connecting Space: "+measurement.getSchemaName());
                            logger.debug("Admin API - GS Locator: "+measurement.getDataSourceHostIp());

                            Admin admin = JDBCUtils.getAdminConnection(measurement);
                            String puName = measurement.getSchemaName().replace("space","service");
                            ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(puName, 1, TimeUnit.MINUTES);
                            if(processingUnit == null){
                                this.errorSummary = "Processing unit not available with the name:"+puName;
                                this.result = "Error";
                                return this.result;
                            }
                            Space space = admin.getSpaces().waitFor(measurement.getSchemaName(),1, TimeUnit.MINUTES);
                            logger.debug("Admin API - Space: "+space.toString());
                            for(SpaceInstance se: space.getInstances()){
                                while(se.getMode().equals(SpaceMode.NONE)){
                                    Thread.sleep(400);
                                }
                            }
                            Map<String, Integer> countPerClassName = space.getRuntimeDetails().getCountPerClassName();
                            logger.debug("Admin API - Table count: " + countPerClassName.get(measurement.getTableName()));

                            Integer count = countPerClassName.get(measurement.getTableName());
                            this.result = String.valueOf(count);
                            this.query = "N/A";
                            logger.debug("### Admin API: Count="+this.result);
                            admin.close();
                            return this.result;
					    }
                        if(measurement.getType().equals("max")){
                            logger.debug("### Optimized Get Max value from Gigaspaces ###");
                            this.query = "N/A";
                            if(column_type!=null && column_type.equalsIgnoreCase("java.time.LocalDateTime")){
                                LocalDateTime maxDateTime = LocalDateTime.of(2099,
                                        Month.DECEMBER, 31, 23, 59, 59);

                                AsyncFuture<LocalDateTime> future = gigaSpace.execute(new MaxLocalDateTimeValueTaskNew(maxDateTime ,
                                        measurement.getTableName(),  measurement.getFieldName()));
                                LocalDateTime dateVal = future.get();
                                logger.debug("### Get Max ["+column_type+"] value from Gigaspaces is "+dateVal);
                                if(dateVal == null){
                                    this.result = "FAIL";
                                    this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                    return this.result;
                                }
                                String dateStr = dateVal.toString();
                                String dateFormat = "yyyy-MM-dd HH:mm:ss";
                                if(StringUtils.countOccurrencesOf(dateStr,":")==1){
                                    dateFormat = "yyyy-MM-dd HH:mm";
                                }
                                if (dateStr.indexOf("T") > 0) {
                                    dateFormat=dateFormat.replaceAll(" ","'T'");
                                }
                                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                                Date date = sdf.parse(dateStr);
                                this.result = String.valueOf(date.getTime());
                            }else if(column_type!=null && column_type.equalsIgnoreCase("java.sql.Date")){
                                java.sql.Date maxDateVal = new java.sql.Date(4102444799000L); //Thursday, 31 December 2099 23:59:59
                                AsyncFuture<java.sql.Date> future = gigaSpace.execute(new MaxSqlDateValueTask(maxDateVal ,
                                        measurement.getTableName(),  measurement.getFieldName()));
                                java.sql.Date dateVal = future.get();
                                if(dateVal == null){
                                    this.result = "FAIL";
                                    this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                    return this.result;
                                }
                                this.result = String.valueOf(dateVal.getTime());
                            }else if (column_type!=null && column_type.equalsIgnoreCase("java.sql.Timestamp")){
                                Timestamp maxDateVal = new Timestamp(4102444799000L); //Thursday, 31 December 2099 23:59:59
                                AsyncFuture<java.sql.Timestamp> future = gigaSpace.execute(new MaxTimestampValueTask(maxDateVal ,
                                        measurement.getTableName(),  measurement.getFieldName()));
                                java.sql.Timestamp dateVal = future.get();
                                if(dateVal == null){
                                    this.result = "FAIL";
                                    this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                    return this.result;
                                }
                                this.result = String.valueOf(dateVal.getTime());
                            }else if (column_type!=null && column_type.equalsIgnoreCase("java.lang.Integer")){
                                    AsyncFuture<Integer> future = gigaSpace.execute(new MaxValueTask<Integer>(Integer.MAX_VALUE ,
                                            measurement.getTableName(),  measurement.getFieldName()));
                                    Integer colVal = future.get();
                                    if(colVal == null){
                                        this.result = "FAIL";
                                        this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                        return this.result;
                                    }
                                    this.result = String.valueOf(colVal);
                            }else if (column_type!=null && column_type.equalsIgnoreCase("java.lang.Long")){
                                    AsyncFuture<Long> future = gigaSpace.execute(new MaxValueTask<Long>(Long.MAX_VALUE ,
                                            measurement.getTableName(),  measurement.getFieldName()));
                                    Long colVal = future.get();
                                    if(colVal == null){
                                        this.result = "FAIL";
                                        this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                        return this.result;
                                    }
                                    this.result = String.valueOf(colVal);
                            }else if (column_type!=null && column_type.equalsIgnoreCase("java.lang.Float")){
                                    AsyncFuture<Float> future = gigaSpace.execute(new MaxValueTask<Float>(Float.MAX_VALUE ,
                                            measurement.getTableName(),  measurement.getFieldName()));
                                    Float colVal = future.get();
                                    if(colVal == null){
                                        this.result = "FAIL";
                                        this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                        return this.result;
                                    }
                                    this.result = String.valueOf(colVal);
                            }else if (column_type!=null && column_type.equalsIgnoreCase("java.lang.Double")){
                                    AsyncFuture<Double> future = gigaSpace.execute(new MaxValueTask<Double>(Double.MAX_VALUE ,
                                            measurement.getTableName(),  measurement.getFieldName()));
                                    Double colVal = future.get();
                                    if(colVal == null){
                                        this.result = "FAIL";
                                        this.errorSummary="Table ['"+measurement.getTableName()+"'] is empty";
                                        return this.result;
                                    }
                                    this.result = String.valueOf(colVal);
                            }
                            return this.result;
                        }
                    }

					Connection conn = JDBCUtils.getConnection(measurement);
					Statement st = conn.createStatement();
					String query = JDBCUtils.buildQuery(measurement.getDataSourceType(), measurement.getFieldName(),
							measurement.getType(), measurement.getTableName(),
							Long.parseLong(measurement.getLimitRecords()), whereCondition);
					logger.debug("query: " + query);
					ResultSet rs = st.executeQuery(query);

					if(!measurement.getDataSourceType().equals("gigaspaces")) {
						//Retrieving the ResultSetMetaData object
						ResultSetMetaData rsmd = rs.getMetaData();
						try {
							column_type = rsmd.getColumnClassName(1);
						} catch (Exception e) {
							logger.error("Error in getting column data type so assuming string");
						}
					}
					String val = "";
					while (rs.next()) {
						logger.debug("Column Type:     " + column_type);
						if(column_type!=null
								&& (column_type.equalsIgnoreCase("java.sql.Timestamp"))
						){
							val = String.valueOf(rs.getTimestamp(1).getTime());
						}else if(column_type.equalsIgnoreCase("java.sql.Date")) {
							val = String.valueOf(rs.getDate(1).getTime());
						}else if( column_type.equalsIgnoreCase("java.time.LocalDateTime")){
                            String dateStr = rs.getString(1);
                            String dateFormat = "yyyy-MM-dd HH:mm:ss";
                            if(StringUtils.countOccurrencesOf(dateStr,":")==1){
                                dateFormat = "yyyy-MM-dd HH:mm";
                            }
                            if (dateStr.indexOf("T") > 0) {
                                dateFormat=dateFormat.replaceAll(" ","'T'");
                            }
                            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                            Date date = sdf.parse(dateStr);
                            val= String.valueOf(date.getTime());
                            logger.debug("date final val: " + dateStr);
                        }else{
							val = rs.getString(1);
						}
						logger.debug("val:     " + val);
					}
					this.result = String.valueOf(val);
					this.query = query;
				} else {
					this.result = "FAIL";
					this.errorSummary="Measurement not available";
				}


		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
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