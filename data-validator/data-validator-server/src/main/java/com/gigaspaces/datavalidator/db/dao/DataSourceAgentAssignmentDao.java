package com.gigaspaces.datavalidator.db.dao;

import com.gigaspaces.datavalidator.model.Agent;
import com.gigaspaces.datavalidator.model.DataSourceAgentAssignment;

import java.util.List;

public interface DataSourceAgentAssignmentDao extends DAO<DataSourceAgentAssignment> {
    List<DataSourceAgentAssignment> getActiveDataSourceAgents();
    List<DataSourceAgentAssignment> getByDataSource(long dataSourceId);

}
