package com.gigaspaces.common.task;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

public class MaxTimestampValueTask implements DistributedTask<Timestamp,Timestamp> {

    private Timestamp maxParameter;
    private String tableName;
    private String columnName;

    public MaxTimestampValueTask(Timestamp maxParameter, String tableName, String columnName) {
        this.maxParameter = maxParameter;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    @Override
    public Timestamp execute() throws Exception {
        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<SpaceDocument>(this.tableName, this.columnName + " < ? AND rownum <= 1");
        sqlQuery.setParameter(1, maxParameter);
        sqlQuery.setProjections(this.columnName);
        SpaceDocument result = gigaSpace.read(sqlQuery);
        if(result == null || result.getProperty(this.columnName) == null){
            return null;
        }else {
            return (Timestamp) result.getProperty(this.columnName);
        }
    }

    @Override
    public Timestamp reduce(List<AsyncResult<Timestamp>> results) throws Exception {
        List<Timestamp> resultList = new LinkedList<>();
        for (AsyncResult<Timestamp> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            if(result.getResult()!=null) {
                resultList.add(result.getResult());
            }else {
                return null;
            }
        }
        return getMax(resultList);
    }
    public Timestamp getMax(List<Timestamp> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
