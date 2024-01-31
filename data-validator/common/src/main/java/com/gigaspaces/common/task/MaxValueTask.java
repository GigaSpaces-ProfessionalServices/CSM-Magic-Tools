package com.gigaspaces.common.task;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.core.Link;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

public class MaxValueTask<T extends Serializable & Comparable<T>> implements DistributedTask<T,T> {

    private T maxParameter;
    private String tableName;
    private String columnName;

    public MaxValueTask(T maxParameter, String tableName, String columnName) {
        this.maxParameter = maxParameter;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    @Override
    public T execute() throws Exception {
        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<SpaceDocument>(this.tableName, this.columnName + " < ? AND rownum <= 1");
        sqlQuery.setParameter(1, maxParameter);
        sqlQuery.setProjections(this.columnName);
        SpaceDocument result = gigaSpace.read(sqlQuery);
        if(result == null || result.getProperty(this.columnName) == null){
            return null;
        }else {
            return (T) result.getProperty(this.columnName);
        }
    }

    @Override
    public T reduce(List<AsyncResult<T>> results) throws Exception {
        List<T> resultList = new LinkedList<>();
        for (AsyncResult<T> result : results) {
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
    public <T extends Comparable<T>> T getMax(List<T> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
