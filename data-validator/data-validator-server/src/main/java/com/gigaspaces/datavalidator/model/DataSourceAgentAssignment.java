package com.gigaspaces.datavalidator.model;

import java.io.Serializable;

public class DataSourceAgentAssignment implements Serializable{

	public DataSourceAgentAssignment() {
	}
	private long dataSourceId;
	private long agentId;

	public DataSourceAgentAssignment(long dataSourceId,long agentId) {
		super();
		this.dataSourceId = dataSourceId;
		this.agentId = agentId;
	}

	public long getDataSourceId() {
		return dataSourceId;
	}

	public void setDataSourceId(long dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public long getAgentId() {
		return agentId;
	}

	public void setAgentId(long agentId) {
		this.agentId = agentId;
	}
}
