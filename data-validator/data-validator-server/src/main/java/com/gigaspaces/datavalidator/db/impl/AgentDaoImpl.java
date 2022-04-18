package com.gigaspaces.datavalidator.db.impl;

import com.gigaspaces.datavalidator.db.dao.AgentDao;
import com.gigaspaces.datavalidator.model.Agent;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AgentDaoImpl extends DAOImplAbstract<Agent> implements AgentDao{

	@Override
	public List<Agent> getActiveAgents() {
		String sql = "from com.gigaspaces.datavalidator.model.Agent";
		return getAll(sql);
	}

}
