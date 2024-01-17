package com.gigaspaces.datavalidator.model;


import com.gigaspaces.datavalidator.db.InfluxDbProperties;
import com.gigaspaces.datavalidator.utils.EncryptionDecryptionUtils;
import com.gigaspaces.datavalidator.utils.InfluxDBUtils;
import com.gigaspaces.datavalidator.utils.JDBCUtils;
import com.gigaspaces.datavalidator.utils.NetClientPost;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class TestTask  implements Serializable  {

	private static Logger logger = LoggerFactory.getLogger(TestTask.class.getName());

	//private long id;
	private String uuid;
	private long time;
	private String type;
    private String testOption;
	private String result = null;
	private String measurementIds;
	private String errorSummary;
	private String summary;
	private String query;
	private List<Measurement> measurementList;
	private boolean influxdbResultStore;
	private InfluxDbProperties influxDbProperties;

	public TestTask() {
	}

    public TestTask(String uuid, long time, String testOption, List<Measurement> measurementList
            ,boolean influxdbResultStore,InfluxDbProperties influxDbProperties) {
        this(uuid, time, testOption, measurementList
            , influxdbResultStore, influxDbProperties, null);
    }
	public TestTask(String uuid, long time, String testOption, List<Measurement> measurementList
			,boolean influxdbResultStore,InfluxDbProperties influxDbProperties, String type) {

		this.uuid = uuid;
		this.time = time;
		this.type = type;
        this.testOption = testOption;
		this.measurementList = measurementList;
		this.result = "pending";
		this.influxdbResultStore = influxdbResultStore;
		this.influxDbProperties = influxDbProperties;

		String measurementIds = null;

		for (int index = 0; index < measurementList.size(); index++) {

			Measurement measurement = measurementList.get(index);

			if (measurement != null) {

				if (index == 0) {
					measurementIds = measurement.getId() + "";
				} else {
					measurementIds = measurementIds + "," + measurement.getId();
				}

			}

		}

		this.measurementIds = measurementIds;

	}


	public String executeTask() {

		Measurement measurement1 = null;
		Measurement measurement2 = null;

		try {
			logger.debug("TestOption:  "+this.testOption);
			if (this.testOption != null && this.testOption.equals("Measure")) {

				Measurement measurement = measurementList.get(0);
				if (measurement != null) {
					DataSource dataSource = measurement.getDataSource();
					logger.debug("Executing task id: " + uuid);
					String whereCondition = measurement.getWhereCondition() != null ? measurement.getWhereCondition()
							: "";

					logger.debug("DataSource: "+dataSource);
					logger.debug("Agent: "+dataSource.getAgent());
					logger.debug("Agent Host: "+dataSource.getAgent().getHostIp());
					String endPoint = "http://"+dataSource.getAgent().getHostIp()+":3223/measurement/run";
					String data= "{" +
							"\"measurementId\":\""+measurement.getId()+"\"" +
							",\"test\":\""+measurement.getType()+"\"" +
							",\"type\":\""+measurement.getType()+"\"" +
							",\"schemaName\":\""+measurement.getSchemaName()+"\"" +
							",\"tableName\":\""+measurement.getTableName()+"\"" +
							",\"fieldName\":\""+measurement.getFieldName()+"\"" +
							",\"whereCondition\":\""+measurement.getWhereCondition()+"\"" +
							",\"limitRecords\":\""+measurement.getLimitRecords()+"\"" +
							",\"dataSourceType\":\""+dataSource.getDataSourceType()+"\"" +
							",\"dataSourceHostIp\":\""+EncryptionDecryptionUtils.encrypt(dataSource.getDataSourceHostIp())+"\"" +
							",\"dataSourcePort\":\""+EncryptionDecryptionUtils.encrypt(dataSource.getDataSourcePort())+"\"" +
							",\"username\":\""+EncryptionDecryptionUtils.encrypt(dataSource.getUsername())+"\"" +
							",\"password\":\""+EncryptionDecryptionUtils.encrypt(dataSource.getPassword())+"\"" +
							",\"integratedSecurity\":\""+dataSource.getIntegratedSecurity()+"\"" +
							",\"properties\":\""+dataSource.getProperties()+"\"" +
							",\"authenticationScheme\":\""+dataSource.getAuthenticationScheme()+"\"" +
                            ",\"gsLookupGroup\":\""+dataSource.getGsLookupGroup()+"\"" +
                            ",\"gsLookupGroup\":\""+dataSource.getGsLookupGroup()+"\"" +
							"}";

					this.result=NetClientPost.send(endPoint,data);
					ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
					objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);

					JsonObject testTaskResponse = (JsonObject) JsonParser.parseString(this.result);
					this.result = testTaskResponse.get("result").getAsString();//testTaskResponse.getResult();
					this.errorSummary = testTaskResponse.get("errorSummary").getAsString();//testTaskResponse.getErrorSummary();
					this.query = testTaskResponse.get("query").getAsString();//testTaskResponse.getQuery();
					/*if(this.result.contains("#") && this.result.contains("Fail")){
						this.errorSummary=this.result.split("#")[1];
						this.result=this.result.split("#")[0];
					}*/

					/*Connection conn = JDBCUtils.getConnection(measurement);
					Statement st = conn.createStatement();
					String query = JDBCUtils.buildQuery(dataSource.getDataSourceType(), measurement.getFieldName(),
							measurement.getType(), measurement.getTableName(),
							Long.parseLong(measurement.getLimitRecords()), whereCondition);
					logger.debug("query: " + query);
					// st = conn.createStatement();
					ResultSet rs = st.executeQuery(query);

					String val = "";
					while (rs.next()) {
						val = rs.getString(1);
						logger.debug("val:     " + val);
					}
					this.result = String.valueOf(val);*/
				} else {
					this.result = "FAIL";
					this.errorSummary = "Measurement not available";
				}
			} else if (this.testOption != null && this.testOption.equals("Compare")) {

				measurement1 = measurementList.get(0);
				measurement2 = measurementList.get(1);

				if (measurement1 != null && measurement2 != null) {
					DataSource dataSource1 = measurement1.getDataSource();
					DataSource dataSource2 = measurement2.getDataSource();
					String test1 = measurement1.getType();
					String test2 = measurement2.getType();
					String limitRecords = measurement1.getLimitRecords();
					String whereCondition = measurement1.getWhereCondition();

					String endPoint = "http://"+dataSource1.getAgent().getHostIp()+":3223/measurement/run";
					String data= "{" +
							"\"measurementId\":\""+measurement1.getId()+"\"" +
							",\"test\":\""+measurement1.getType()+"\"" +
							",\"type\":\""+measurement1.getType()+"\"" +
							",\"schemaName\":\""+measurement1.getSchemaName()+"\"" +
							",\"tableName\":\""+measurement1.getTableName()+"\"" +
							",\"fieldName\":\""+measurement1.getFieldName()+"\"" +
							",\"whereCondition\":\""+measurement1.getWhereCondition()+"\"" +
							",\"limitRecords\":\""+measurement1.getLimitRecords()+"\"" +
							",\"dataSourceType\":\""+dataSource1.getDataSourceType()+"\"" +
							",\"dataSourceHostIp\":\""+EncryptionDecryptionUtils.encrypt(dataSource1.getDataSourceHostIp())+"\"" +
							",\"dataSourcePort\":\""+EncryptionDecryptionUtils.encrypt(dataSource1.getDataSourcePort())+"\"" +
							",\"username\":\""+EncryptionDecryptionUtils.encrypt(dataSource1.getUsername())+"\"" +
							",\"password\":\""+EncryptionDecryptionUtils.encrypt(dataSource1.getPassword())+"\"" +			",\"integratedSecurity\":\""+dataSource1.getIntegratedSecurity()+"\"" +
							",\"properties\":\""+dataSource1.getProperties()+"\"" +
							",\"authenticationScheme\":\""+dataSource1.getAuthenticationScheme()+"\"" +
                            ",\"gsLookupGroup\":\""+dataSource1.getGsLookupGroup()+"\"" +
                            ",\"keepConnectionOpen\":\"true\"" +
							"}";

					String response1=NetClientPost.send(endPoint,data);
					logger.debug("response1: "+response1);
					JsonObject testTaskResponse1 = (JsonObject) JsonParser.parseString(response1);
					response1 = testTaskResponse1.get("result").getAsString();
					this.errorSummary = testTaskResponse1.get("errorSummary").getAsString();
					this.query = testTaskResponse1.get("query").getAsString();


					/*Connection conn2 = JDBCUtils.getConnection(measurement2);

					Statement statement2 = conn2.createStatement();
					String query2 = JDBCUtils.buildQuery(dataSource2.getDataSourceType(), measurement2.getFieldName(),
							test2, measurement2.getTableName(), Long.parseLong(limitRecords), whereCondition);
					logger.debug("query2: " + query2);
					ResultSet resultSet2 = statement2.executeQuery(query2);

					float val2 = 0;
					while (resultSet2.next()) {
						val2 = resultSet2.getFloat(1);
						logger.debug("val2:     " + val2);
					}*/

					logger.debug("DataSource2: "+dataSource2);
					logger.debug("Agent2: "+dataSource2.getAgent());
					logger.debug("Agent2 Host: "+dataSource2.getAgent().getHostIp());
					String endPoint2 = "http://"+dataSource2.getAgent().getHostIp()+":3223/measurement/run";
					String data2= "{" +
							"\"measurementId\":\""+measurement2.getId()+"\"" +
							",\"test\":\""+measurement2.getType()+"\"" +
							",\"type\":\""+measurement2.getType()+"\"" +
							",\"schemaName\":\""+measurement2.getSchemaName()+"\"" +
							",\"tableName\":\""+measurement2.getTableName()+"\"" +
							",\"fieldName\":\""+measurement2.getFieldName()+"\"" +
							",\"whereCondition\":\""+measurement2.getWhereCondition()+"\"" +
							",\"limitRecords\":\""+measurement2.getLimitRecords()+"\"" +
							",\"dataSourceType\":\""+dataSource2.getDataSourceType()+"\"" +
							",\"dataSourceHostIp\":\""+EncryptionDecryptionUtils.encrypt(dataSource2.getDataSourceHostIp())+"\"" +
							",\"dataSourcePort\":\""+EncryptionDecryptionUtils.encrypt(dataSource2.getDataSourcePort())+"\"" +
							",\"username\":\""+EncryptionDecryptionUtils.encrypt(dataSource2.getUsername())+"\"" +
							",\"password\":\""+EncryptionDecryptionUtils.encrypt(dataSource2.getPassword())+"\"" +
							",\"integratedSecurity\":\""+dataSource2.getIntegratedSecurity()+"\"" +
							",\"properties\":\""+dataSource2.getProperties()+"\"" +
							",\"authenticationScheme\":\""+dataSource2.getAuthenticationScheme()+"\"" +
							",\"gsLookupGroup\":\""+dataSource2.getGsLookupGroup()+"\"" +
							"}";

					String response2=NetClientPost.send(endPoint2,data2);
					logger.debug("response2: "+response2);
					JsonObject testTaskResponse2 = (JsonObject) JsonParser.parseString(response2);
					response2 = testTaskResponse2.get("result").getAsString();
					this.query += " | "+testTaskResponse2.get("query").getAsString();
					this.errorSummary += " | " +testTaskResponse2.get("errorSummary").getAsString();


					if(!response1.contains("FAIL") && !response2.contains("FAIL")){
						if (Float.parseFloat(response1) == Float.parseFloat(response2)) {
							this.result = "PASS";
							this.summary = "Results matched. "+dataSource1.getDataSourceType()+"-Result: "+response1 +"  "+dataSource2.getDataSourceType()+"-Result: "+response2;
						} else {
							logger.debug("==> Test Result: FAIL, Test type: " + test1 + ", DataSource1 Result: " + response1
									+ ", DataSource2 Result: " + response2);
							this.result = "FAIL";
							this.errorSummary = "Results not matched. "+dataSource1.getDataSourceType()+"-Result: "+response1 +"  "+dataSource2.getDataSourceType()+"-Result: "+response2;
						}
					}else{
						this.result = "FAIL";
					}

					if(this.influxdbResultStore) {
						// Add Compare Results to InfluxDB
						InfluxDBUtils.doConnect(influxDbProperties.getInfluxDBUrl(), influxDbProperties.getInfluxDBUsername(), influxDbProperties.getInfluxDBPassword());  // TODO: Call this once when application loads or first compare test executes
						InfluxDBUtils.write(influxDbProperties.getInfluxDBName(), "dvState"
								, influxDbProperties.getEnvName(), measurement1.getTableName()
								, this.result,influxDbProperties.getHost());
					}
				}
            } else if(this.testOption != null && this.testOption.equals("BatchCompare")) {
                String key=null;
                Map<String,Measurement> mapA = new LinkedHashMap<>();
                Map<String,Measurement> mapB = new LinkedHashMap<>();
                for(Measurement measurement : measurementList) {
                    if(this.type == null || this.type.equals(measurement.getType())) {
                        key = measurement.getTableName() + "," + measurement.getType();
                        if (measurement.getDataSource().getDataSourceType().equals("gigaspaces")) {
                            mapA.put(key, measurement);
                        } else {
                            mapB.put(key, measurement);
                        }
                    }
                }
                logger.debug("Total Measurements count:"+measurementList.size());
                logger.debug("Measurements A count:"+mapA.size());
                logger.debug("Measurements B count:"+mapB.size());

                Map<Long, Properties> results = new HashMap<>();
                for(Measurement measurement : measurementList) {
                    DataSource dataSource = measurement.getDataSource();
                    String test = measurement.getType();
                    String limitRecords = measurement.getLimitRecords();
                    String whereCondition = measurement.getWhereCondition();

                    String endPoint = "http://" + dataSource.getAgent().getHostIp() + ":3223/measurement/run";
                    String data = "{" + "\"measurementId\":\"" + measurement.getId() + "\"" + ",\"test\":\""
                            + measurement.getType() + "\"" + ",\"type\":\"" + measurement.getType() + "\""
                            + ",\"schemaName\":\"" + measurement.getSchemaName() + "\"" + ",\"tableName\":\""
                            + measurement.getTableName() + "\"" + ",\"fieldName\":\"" + measurement.getFieldName()
                            + "\"" + ",\"whereCondition\":\"" + measurement.getWhereCondition() + "\""
                            + ",\"limitRecords\":\"" + measurement.getLimitRecords() + "\"" + ",\"dataSourceType\":\""
                            + dataSource.getDataSourceType() + "\"" + ",\"dataSourceHostIp\":\""
                            + EncryptionDecryptionUtils.encrypt(dataSource.getDataSourceHostIp()) + "\""
                            + ",\"dataSourcePort\":\"" + EncryptionDecryptionUtils.encrypt(
                            dataSource.getDataSourcePort()) + "\"" + ",\"username\":\""
                            + EncryptionDecryptionUtils.encrypt(dataSource.getUsername()) + "\"" + ",\"password\":\""
                            + EncryptionDecryptionUtils.encrypt(dataSource.getPassword()) + "\""
                            + ",\"integratedSecurity\":\"" + dataSource.getIntegratedSecurity() + "\""
                            + ",\"properties\":\"" + dataSource.getProperties() + "\"" + ",\"authenticationScheme\":\""
                            + dataSource.getAuthenticationScheme() + "\"" + ",\"gsLookupGroup\":\""
                            + dataSource.getGsLookupGroup() + "\"" + ",\"keepConnectionOpen\":\"true\"" + "}";

                    String response = NetClientPost.send(endPoint, data);
                    logger.debug("response: " + response);
                    JsonObject testTaskResponse = (JsonObject) JsonParser.parseString(response);
                    response = testTaskResponse.get("result").getAsString();
                    this.errorSummary = testTaskResponse.get("errorSummary").getAsString();
                    this.query = testTaskResponse.get("query").getAsString();

                    Properties result = new Properties();
                    result.put("result",response);
                    result.put("query",this.query);
                    result.put("errorSummary",this.errorSummary);
                    result.put("dataSourceType",dataSource.getDataSourceType());
                    result.put("measurementType",measurement.getType());
                    results.put(measurement.getId(),result);
                }

                Map<String, Properties> finalResults = new HashMap<>();
                Measurement measurementA = null;
                Measurement measurementB = null;
                String compareResult = "";
                String compareResultSummary = "";
                String compareErrorSummary = "";
                for(String keyA:mapA.keySet()){
                    measurementA=mapA.get(keyA);
                    measurementB=mapB.get(keyA);
                    if(measurementA!=null && measurementB!=null){
                        if(!results.get(measurementA.getId()).getProperty("result").contains("FAIL")
                                && !results.get(measurementB.getId()).getProperty("result").contains("FAIL")){
                            if (Float.parseFloat(results.get(measurementA.getId()).getProperty("result")) == Float.parseFloat(results.get(measurementB.getId()).getProperty("result"))) {
                                compareResult = "PASS";
                                compareResultSummary = "Results matched. "
                                        +results.get(measurementA.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementA.getId()).getProperty("result")
                                        +"  "+results.get(measurementB.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementB.getId()).getProperty("result");
                            } else {
                                logger.debug("==> Test Result: FAIL, Test type: "
                                        + results.get(measurementA.getId()).getProperty("measurementType")
                                        + ", "+results.get(measurementA.getId()).getProperty("dataSourceType")+"-Result: " + results.get(measurementA.getId()).getProperty("result")
                                        + ", "+results.get(measurementB.getId()).getProperty("dataSourceType")+"-Result: " + results.get(measurementB.getId()).getProperty("result"));
                                compareResult = "FAIL";
                                compareErrorSummary = "Results not matched. "
                                        +results.get(measurementA.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementA.getId()).getProperty("result") +"  "+results.get(measurementB.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementB.getId()).getProperty("result");
                            }
                        }else{
                            compareResult = "FAIL";
                            compareErrorSummary = "Results not matched. "
                                    +results.get(measurementA.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementA.getId()).getProperty("result") +"  "+results.get(measurementB.getId()).getProperty("dataSourceType")+"-Result: "+results.get(measurementB.getId()).getProperty("result");
                        }
                        Properties res= new Properties();
                        res.setProperty("result",compareResult);
                        res.setProperty("summary",compareResultSummary);
                        res.setProperty("errorSummary",compareErrorSummary);
                        res.setProperty("type",measurementA.getType());
                        res.setProperty("tableName",measurementA.getTableName());

                        finalResults.put(measurementA.getId()+","+measurementB.getId(),res);
                    }else{
                        logger.error("Measurement with key: "+keyA+" not available in compare list");
                    }

                }

                String finalResult = new ObjectMapper().writeValueAsString(finalResults);
                logger.debug("Final Results of Batch Execute: "+finalResult);
                System.out.println(finalResult);
                this.result=finalResult;
            }else {
				this.result = "Incorrect type";
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.debug(e.getMessage());
			this.errorSummary = e.getMessage();
			this.result = "FAIL";
			return this.result;
		}
		if (measurement1 == null || measurement2 == null) {
			if ((this.testOption != null && this.testOption.equals("Compare"))) {
				this.errorSummary = "Invalid input for measurements ";
				this.result = "FAIL";
				this.errorSummary = "Measurement not available";
			}
		}
		logger.debug("In TestTask Last line: "+this.result);
		return this.result;
	}
	public String executeTaskOld() {

		String testType = this.type;
		Measurement measurement1 = null;
		Measurement measurement2 = null;

		try {

			if (testType != null && testType.equals("Measure")) {

				Measurement measurement = measurementList.get(0);
				if (measurement != null) {
					DataSource dataSource = measurement.getDataSource();
					logger.debug("Executing task id: " + uuid);
					String whereCondition = measurement.getWhereCondition() != null ? measurement.getWhereCondition()
							: "";

					Connection conn = JDBCUtils.getConnection(measurement);
					Statement st = conn.createStatement();
					String query = JDBCUtils.buildQuery(dataSource.getDataSourceType(), measurement.getFieldName(),
							measurement.getType(), measurement.getTableName(),
							Long.parseLong(measurement.getLimitRecords()), whereCondition);
					logger.debug("query: " + query);
					// st = conn.createStatement();
					ResultSet rs = st.executeQuery(query);

					String val = "";
					while (rs.next()) {
						val = rs.getString(1);
						logger.debug("val:     " + val);
					}
					this.result = String.valueOf(val);
				} else {
					this.result = "FAIL";
					this.errorSummary = "Measurement not available";
				}
			} else if (testType != null && testType.equals("Compare")) {

				measurement1 = measurementList.get(0);
				measurement2 = measurementList.get(1);

				if (measurement1 != null && measurement2 != null) {
					DataSource dataSource1 = measurement1.getDataSource();
					DataSource dataSource2 = measurement2.getDataSource();
					String test1 = measurement1.getType();
					String test2 = measurement2.getType();
					String limitRecords = measurement1.getLimitRecords();
					String whereCondition = measurement1.getWhereCondition();

					Connection conn1 = JDBCUtils.getConnection(measurement1);

					Statement statement1 = conn1.createStatement();
					String query1 = JDBCUtils.buildQuery(dataSource1.getDataSourceType(), measurement1.getFieldName(),
							test1, measurement1.getTableName(), Long.parseLong(limitRecords), whereCondition);
					logger.debug("query1: " + query1);
					ResultSet resultSet1 = statement1.executeQuery(query1);

					float val1 = 0;
					while (resultSet1.next()) {
						val1 = resultSet1.getFloat(1);
						logger.debug("val1:     " + val1);
					}

					Connection conn2 = JDBCUtils.getConnection(measurement2);

					Statement statement2 = conn2.createStatement();
					String query2 = JDBCUtils.buildQuery(dataSource2.getDataSourceType(), measurement2.getFieldName(),
							test2, measurement2.getTableName(), Long.parseLong(limitRecords), whereCondition);
					logger.debug("query2: " + query2);
					ResultSet resultSet2 = statement2.executeQuery(query2);

					float val2 = 0;
					while (resultSet2.next()) {
						val2 = resultSet2.getFloat(1);
						logger.debug("val2:     " + val2);
					}
					if (val1 == val2) {
						this.result = "PASS";
					} else {
						logger.debug("==> Test Result: FAIL, Test type: " + test1 + ", DataSource1 Result: " + val1
								+ ", DataSource2 Result: " + val2);
						this.result = "FAIL";
						this.errorSummary = "Results not matched. Result-1: "+val1 +"  Result-2: "+val2;

					}
				}
			} else {

				this.result = "Incorrect type";

			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.debug(e.getMessage());
			this.errorSummary = e.getMessage();
			this.result = "FAIL";
			return this.result;
		}
		if (measurement1 == null || measurement2 == null) {
			if ((testType != null && testType.equals("Compare"))) {
				this.errorSummary = "Invalid input for measurements ";
				this.result = "FAIL";
			}
		}

		return this.result;
	}



	public String getResult() {
		return (result == null) ? "pending" : result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String toString() {
		return "[" + uuid + " " + result + "]";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Measurement> getMeasurementList() {
		return measurementList;
	}

	public void setMeasurementList(List<Measurement> measurementList) {
		this.measurementList = measurementList;
	}

	public String getMeasurementIds() {
		return measurementIds;
	}

	public void setMeasurementIds(String measurementIds) {
		this.measurementIds = measurementIds;
	}

	public String getErrorSummary() {
		return errorSummary;
	}

	public void setErrorSummary(String errorSummary) {
		this.errorSummary = errorSummary;
	}

	/*public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}*/

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public InfluxDbProperties getInfluxDbProperties() {
		return influxDbProperties;
	}

	public void setInfluxDbProperties(InfluxDbProperties influxDbProperties) {
		this.influxDbProperties = influxDbProperties;
	}

    public String getTestOption() {
        return testOption;
    }

    public void setTestOption(String testOption) {
        this.testOption = testOption;
    }

    public boolean isInfluxdbResultStore() {
		return influxdbResultStore;
	}

	public void setInfluxdbResultStore(boolean influxdbResultStore) {
		this.influxdbResultStore = influxdbResultStore;
	}

}