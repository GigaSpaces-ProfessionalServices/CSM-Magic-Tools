package com.gigaspaces.datavalidator.db.service;

import com.gigaspaces.datavalidator.db.dao.AgentDao;
import com.gigaspaces.datavalidator.db.dao.DataSourceDao;
import com.gigaspaces.datavalidator.model.Agent;
import com.gigaspaces.datavalidator.model.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AgentService {

	@Autowired
	private AgentDao agentDao;

	public void update(Agent agent) {
		agentDao.update(agent);
	}

	public void add(Agent agent) {
		agentDao.add(agent);
	}

	public void deleteById(long id) {
		agentDao.deleteById(id);
	}

	public long getAutoIncId() {
		return agentDao.getAutoIncId("AGENT", "AGENT_ID");
	}

	public List<Agent> getActiveAgents() {
		return agentDao.getActiveAgents();
	}
	
	public List<Agent> getAll() {
		return agentDao.getAll();
	}
	public Agent getById(long id) {
		return agentDao.getById(id);
	}
	public List<Agent> getByHostIp(String hostIp) {
		return agentDao.getByHostIp(hostIp);
	}


}
