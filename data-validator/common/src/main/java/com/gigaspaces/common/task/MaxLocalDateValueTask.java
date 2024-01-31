package com.gigaspaces.common.task;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

public class MaxLocalDateValueTask implements DistributedTask<LocalDate,LocalDate> {

    private LocalDate maxParameter;
    private String tableName;
    private String columnName;

    public MaxLocalDateValueTask(LocalDate maxParameter, String tableName, String columnName) {
        this.maxParameter = maxParameter;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    @Override
    public LocalDate execute() throws Exception {
        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<SpaceDocument>(this.tableName, this.columnName + " < ? AND rownum <= 1");
        sqlQuery.setParameter(1, maxParameter);
        sqlQuery.setProjections(this.columnName);
        SpaceDocument result = gigaSpace.read(sqlQuery);
        if(result == null || result.getProperty(this.columnName) == null){
            return null;
        }else {
            return (LocalDate) result.getProperty(this.columnName);
        }
    }

    @Override
    public LocalDate reduce(List<AsyncResult<LocalDate>> results) throws Exception {
        List<LocalDate> resultList = new LinkedList<>();
        for (AsyncResult<LocalDate> result : results) {
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
    public LocalDate getMax(List<LocalDate> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
