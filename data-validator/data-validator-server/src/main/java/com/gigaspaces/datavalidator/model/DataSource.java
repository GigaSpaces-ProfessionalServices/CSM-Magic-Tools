package com.gigaspaces.datavalidator.model;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;

import java.io.Serializable;
import java.util.Objects;

//@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DataSource implements Serializable{
	
	public DataSource() {
	}
	private long id;
	private String dataSourceName;
	private String dataSourceType;
	private String dataSourceHostIp;
	private String dataSourcePort;
	private String username;
	private String password;
	private String integratedSecurity;
	private String properties;
	private String authenticationScheme;
	private String gsLookupGroup="xap-16.2.0";
	private String status;
	@JsonManagedReference
	private Agent agent;
	
	public DataSource(long id, String dataSourceName, String dataSourceType, String dataSourceHostIp,
			String dataSourcePort, String username, String password) {
		super();
		this.id = id;
		this.dataSourceName = dataSourceName;
		this.dataSourceType = dataSourceType;
		this.dataSourceHostIp = dataSourceHostIp;
		this.dataSourcePort = dataSourcePort;
		this.username = username;
		this.password = password;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getDataSourceName() {
		return dataSourceName;
	}
	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}
	public String getDataSourceType() {
		return dataSourceType;
	}
	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}
	public String getDataSourceHostIp() {
		return dataSourceHostIp;
	}
	public void setDataSourceHostIp(String dataSourceHostIp) {
		this.dataSourceHostIp = dataSourceHostIp;
	}
	public String getDataSourcePort() {
		return dataSourcePort;
	}
	public void setDataSourcePort(String dataSourcePort) {
		this.dataSourcePort = dataSourcePort;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getIntegratedSecurity() {
		return integratedSecurity;
	}
	public void setIntegratedSecurity(String integratedSecurity) {
		this.integratedSecurity = integratedSecurity;
	}

	public String getProperties() {
		return properties;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}

	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

	public void setAuthenticationScheme(String authenticationScheme) {
		this.authenticationScheme = authenticationScheme;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Agent getAgent() {
		return agent;
	}

	public void setAgent(Agent agent) {
		this.agent = agent;
	}

	public String getGsLookupGroup() {
		return gsLookupGroup;
	}

	public void setGsLookupGroup(String gsLookupGroup) {
		this.gsLookupGroup = gsLookupGroup;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DataSource that = (DataSource) o;
		return id == that.id && dataSourceName.equals(that.dataSourceName) && dataSourceType.equals(that.dataSourceType) && dataSourceHostIp.equals(that.dataSourceHostIp) && dataSourcePort.equals(that.dataSourcePort) && Objects.equals(username, that.username) && Objects.equals(password, that.password) && Objects.equals(integratedSecurity, that.integratedSecurity) && Objects.equals(properties, that.properties) && Objects.equals(authenticationScheme, that.authenticationScheme) && Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
