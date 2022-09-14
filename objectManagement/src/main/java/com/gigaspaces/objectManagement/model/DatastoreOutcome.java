package com.gigaspaces.objectManagement.model;

import java.util.Map;
import java.util.TreeMap;

public class DatastoreOutcome {
    Map<String, TableOutcome> outcomeMap;
    
    public DatastoreOutcome(){
        outcomeMap = new TreeMap<>() ;
    }
    
    public boolean setRecord(String type, RecordOutcome outcome){
        outcomeMap.computeIfAbsent(type, (missingTypeName) -> {
            TableOutcome tableOutcome = new TableOutcome(missingTypeName);
            return tableOutcome;
        });
        return true;
    }
}
