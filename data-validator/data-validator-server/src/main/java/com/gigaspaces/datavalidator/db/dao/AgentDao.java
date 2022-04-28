package com.gigaspaces.datavalidator.db.dao;

import com.gigaspaces.datavalidator.model.Agent;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentDao extends DAO<Agent> {
	List<Agent> getActiveAgents();
	List<Agent> getByHostIp(String hostIp);
}
