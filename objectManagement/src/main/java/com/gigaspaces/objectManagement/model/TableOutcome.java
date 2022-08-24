package com.gigaspaces.objectManagement.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TableOutcome {
    //Keep hold of typeName to be able to interpret records
    String typeName;
    // Map of ID value -> Record
    List< RecordOutcome> outcomeList;
    public Map<String, Object> additionalInfo;
    public TableOutcome(String typeName){
        this.typeName = typeName;
        outcomeList = new LinkedList<>();
        additionalInfo = new TreeMap<>();
    }

    public void addRecord(RecordOutcome recordForDoc1) {
        outcomeList.add(recordForDoc1);
    }

    public List<RecordOutcome> records() {
        return outcomeList;
    }
}
