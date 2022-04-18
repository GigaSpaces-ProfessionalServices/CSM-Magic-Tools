package com.gigaspaces.datavalidator.db.impl;

import com.gigaspaces.datavalidator.db.dao.DataSourceAgentAssignmentDao;
import com.gigaspaces.datavalidator.model.Agent;
import com.gigaspaces.datavalidator.model.DataSourceAgentAssignment;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DataSourceAgentAssignmentDaoImpl extends DAOImplAbstract<DataSourceAgentAssignment> implements DataSourceAgentAssignmentDao {
    @Override
    public List<DataSourceAgentAssignment> getActiveDataSourceAgents() {
        String sql = "from com.gigaspaces.datavalidator.model.DataSourceAgentAssignment";
        return getAll(sql);
    }
    @Override
    public List<DataSourceAgentAssignment> getByDataSource(long dataSourceId) {
        String sql = "from com.gigaspaces.datavalidator.model.DataSourceAgentAssignment WHERE dataSourceId="+dataSourceId;
        return getAll(sql);
    }
}
