package com.gigaspaces.datavalidator.db.service;

import com.gigaspaces.datavalidator.db.dao.DataSourceAgentAssignmentDao;
import com.gigaspaces.datavalidator.db.dao.DataSourceDao;
import com.gigaspaces.datavalidator.model.Agent;
import com.gigaspaces.datavalidator.model.DataSource;
import com.gigaspaces.datavalidator.model.DataSourceAgentAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DataSourceAgentAssignmentService {

	@Autowired
	private DataSourceAgentAssignmentDao dataSourceAgentAssignmentDao;

	@Autowired
	private AgentService agentService;

	/*public void update(DataSourceAgentAssignment dataSourceAgentAssignment) {
		dataSourceAgentAssignmentDao.update(dataSourceAgentAssignment);
	}

	public void add(DataSourceAgentAssignment dataSourceAgentAssignment) {
		dataSourceAgentAssignmentDao.add(dataSourceAgentAssignment);
	}

	public void deleteByDataSourceId(long dataSourceId) {
		dataSourceAgentAssignmentDao.deleteById(dataSourceId);
	}

	public long getAutoIncId() {
		return dataSourceAgentAssignmentDao.getAutoIncId("DATASOURCE", "DATASOURCE_ID");
	}

	public List<DataSourceAgentAssignment> getActiveDataSourceAgents() {
		return dataSourceAgentAssignmentDao.getActiveDataSourceAgents();
	}
	
	public List<DataSourceAgentAssignment> getAll() {
		return dataSourceAgentAssignmentDao.getAll();
	}

	public DataSourceAgentAssignment getDataSource(long parseLong) {
		return dataSourceAgentAssignmentDao.getById(parseLong);
	}

	public Agent getAgentByDataSource(long dataSourceId){
		List<DataSourceAgentAssignment> dataSourceAgentAssignmentList=dataSourceAgentAssignmentDao.getByDataSource(dataSourceId);
		Agent agent = null;
		if(!dataSourceAgentAssignmentList.isEmpty()){
			agent = agentService.getById(dataSourceAgentAssignmentList.get(0).getAgentId());
		}
		return agent;
	}*/

}
