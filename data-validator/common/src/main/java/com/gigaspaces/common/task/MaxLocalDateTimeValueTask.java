package com.gigaspaces.common.task;

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
        SpaceDocument result = gigaSpace.read(sqlQuery);
        logger.debug("@@@ MaxLocalDateTimeValueTask Executor Task Result: "+(LocalDateTime) result.getProperty(this.columnName));
        System.out.println("@@@ MaxLocalDateTimeValueTask Executor Task Result: "+(LocalDateTime) result.getProperty(this.columnName));
        return (LocalDateTime) result.getProperty(this.columnName);
    }

    @Override
    public LocalDateTime reduce(List<AsyncResult<LocalDateTime>> results) throws Exception {
        List<LocalDateTime> resultList = new LinkedList<>();
        logger.debug("MaxLocalDateTimeValueTask Reducer Total Results: "+results.size());
        for (AsyncResult<LocalDateTime> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            logger.debug("MaxLocalDateTimeValueTask Reducer Individual Result: "+result.getResult());
            resultList.add(result.getResult());
        }
        return getMax(resultList);
    }
    public LocalDateTime getMax(List<LocalDateTime> list) {
        return Collections.max(list, Comparator.naturalOrder());
    }
}
