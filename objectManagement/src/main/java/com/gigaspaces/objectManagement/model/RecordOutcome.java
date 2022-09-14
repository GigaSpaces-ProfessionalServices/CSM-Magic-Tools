package com.gigaspaces.objectManagement.model;

import java.util.Map;
import java.util.TreeMap;

public class RecordOutcome {
    public Object idColumnValue;
    public Map<String, FieldOutcome> outcomeMap;
    public boolean recordWritten;
    public boolean recordRead;
    public boolean comparedEqual;
    public Map<String, Object> additionalInfo;

    public static final String WRITE_EXCEPTION = "WRITE_EXCEPTION";
    public RecordOutcome(){
        outcomeMap = new TreeMap<>();
        additionalInfo = new TreeMap<>();
    }


    public void setWriteResult(String propertyName, boolean b) {
        FieldOutcome fieldOutcome = outcomeMap.computeIfAbsent(propertyName, (key) -> {
            FieldOutcome newFieldOutcome = new FieldOutcome();
            newFieldOutcome.fieldName = key;
            return newFieldOutcome;
        });
        fieldOutcome.fieldName = propertyName;
        fieldOutcome.couldWrite = b;
    }


    public void setRecordWriteResult(boolean b) {
        recordWritten = b;
    }

    public void setRecordWriteException(Exception ex) {
        additionalInfo.put(WRITE_EXCEPTION, ex);
    }


    public void setIdColumn(Object idPropertyValue) {
        this.idColumnValue = idPropertyValue;
    }

    public Object getIdValue() {
        return this.idColumnValue;
    }

    public void setFieldRead(String property, boolean b) {
        outcomeMap.get(property).couldRead = true;
    }
}
