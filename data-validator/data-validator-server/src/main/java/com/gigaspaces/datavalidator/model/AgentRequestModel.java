package com.gigaspaces.datavalidator.model;

public class AgentRequestModel {
	private String agentHostIp;
	private String agentUser;

	public String getAgentHostIp() {
		return agentHostIp;
	}

	public void setAgentHostIp(String agentHostIp) {
		this.agentHostIp = agentHostIp;
	}

	public String getAgentUser() {
		return agentUser;
	}

	public void setAgentUser(String agentUser) {
		this.agentUser = agentUser;
	}

	@Override
	public String toString() {
		return "AgentRequestModel{" +
				"agentHostIp='" + agentHostIp + '\'' +
				", agentUser='" + agentUser + '\'' +
				'}';
	}
}
