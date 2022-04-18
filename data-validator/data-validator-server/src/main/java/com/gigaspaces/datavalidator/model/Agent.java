package com.gigaspaces.datavalidator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonManagedReference;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

//@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Agent implements Serializable{

	public Agent() {
	}
	private long id;
	private String hostIp;
	private String user;
	@JsonBackReference
	private Set<DataSource> dataSources;

	public Agent(long id, String hostIp, String user) {
		super();
		this.id = id;
		this.hostIp = hostIp;
		this.user = user;
		this.dataSources=new HashSet<>();
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Set<DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(Set<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

}
