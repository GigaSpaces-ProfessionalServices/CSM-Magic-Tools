package com.gigaspaces.common.task;

import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import java.sql.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

@SupportCodeChange(id="1")
public class MaxSqlDateValueTask implements DistributedTask<Date,Date> {

    private Date maxParameter;
    private String tableName;
    private String columnName;

    public MaxSqlDateValueTask(Date maxParameter, String tableName, String columnName) {
        this.maxParameter = maxParameter;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    @Override
    public Date execute() throws Exception {
        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<SpaceDocument>(this.tableName, this.columnName + " < ? AND rownum <= 1");
        sqlQuery.setParameter(1, maxParameter);
        sqlQuery.setProjections(this.columnName);
        SpaceDocument result = gigaSpace.read(sqlQuery);
        if(result == null || result.getProperty(this.columnName) == null){
            return null;
        }else {
            return (Date) result.getProperty(this.columnName);
        }
    }

    @Override
    public Date reduce(List<AsyncResult<Date>> results) throws Exception {
        List<Date> resultList = new LinkedList<>();
        for (AsyncResult<Date> result : results) {
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
    public Date getMax(List<Date> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
