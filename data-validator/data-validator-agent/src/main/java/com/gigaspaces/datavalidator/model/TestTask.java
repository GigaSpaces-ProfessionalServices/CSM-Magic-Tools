package com.gigaspaces.datavalidator.model;


import com.gigaspaces.datavalidator.utils.JDBCUtils;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
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

					Connection conn = JDBCUtils.getConnection(measurement);
					Statement st = conn.createStatement();
					String query = JDBCUtils.buildQuery(measurement.getDataSourceType(), measurement.getFieldName(),
							measurement.getType(), measurement.getTableName(),
							Long.parseLong(measurement.getLimitRecords()), whereCondition);
					logger.info("query: " + query);
					ResultSet rs = st.executeQuery(query);

					//Retrieving the ResultSetMetaData object
					ResultSetMetaData rsmd = rs.getMetaData();
					String column_type=null;
					try {
						column_type = rsmd.getColumnClassName(1);
					}catch(Exception e){
						logger.warning("Error in getting column data type so assuming string");
					}
					String val = "";
					while (rs.next()) {
						if(column_type!=null
								&& (column_type.equalsIgnoreCase("java.sql.Timestamp")
						)){
							val = String.valueOf(rs.getTimestamp(1).getTime());
						}else if(column_type.equalsIgnoreCase("java.sql.Date")) {
							val = String.valueOf(rs.getDate(1).getTime());
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