package com.gigaspaces.common.task;

import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SupportCodeChange(id="3")
public class MaxLocalDateTimeValueTask implements DistributedTask<LocalDateTime,LocalDateTime> {

    private static Logger logger = LoggerFactory.getLogger(MaxLocalDateTimeValueTask.class);
    private LocalDateTime maxParameter;
    private String tableName;
    private String columnName;

    public MaxLocalDateTimeValueTask(LocalDateTime maxParameter, String tableName, String columnName) {
        this.maxParameter = maxParameter;
        this.tableName = tableName;
        this.columnName = columnName;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    @Override
    public LocalDateTime execute() throws Exception {
        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<SpaceDocument>(this.tableName, this.columnName + " < ? AND rownum <= 1");
        sqlQuery.setParameter(1, maxParameter);
        sqlQuery.setProjections(this.columnName);
        logger.debug("In Execute method");
        System.out.println("SOP:In Execute method");
        SpaceDocument result = gigaSpace.read(sqlQuery);
        logger.debug("Result from Execute method: "+result);
        System.out.println("SOP:Result from Execute method: "+result);
        if(result == null || result.getProperty(this.columnName) == null){
            logger.debug("Final Result is Null from Execute method");
            System.out.println("SOP:Final Result is Null from Execute method");
            return null;
        }else {
            logger.debug("Final Result from Execute method: "+result.getProperty(this.columnName));
            System.out.println("SOP:Final Result from Execute method: "+result.getProperty(this.columnName));
            return (LocalDateTime) result.getProperty(this.columnName);
        }
    }

    @Override
    public LocalDateTime reduce(List<AsyncResult<LocalDateTime>> results) throws Exception {
        List<LocalDateTime> resultList = new LinkedList<>();
        logger.debug("MaxLocalDateTimeValueTask Reducer Total Results: "+results.size());
        System.out.println("SOP:MaxLocalDateTimeValueTask Reducer Total Results: "+results.size());
        for (AsyncResult<LocalDateTime> result : results) {
            logger.debug("MaxLocalDateTimeValueTask Reducer Individual Result: "+result.getResult());
            System.out.println("SOP:MaxLocalDateTimeValueTask Reducer Individual Result: "+result.getResult());
            if (result.getException() != null) {
                throw result.getException();
            }
            logger.debug("22MaxLocalDateTimeValueTask Reducer Individual Result: "+result.getResult());
            System.out.println("SOP:22MaxLocalDateTimeValueTask Reducer Individual Result: "+result.getResult());
            if(result.getResult()!=null) {
                resultList.add(result.getResult());
            }else {
                return null;
            }
        }
        return getMax(resultList);
    }
    public LocalDateTime getMax(List<LocalDateTime> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
