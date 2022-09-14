package com.gigaspaces.objectManagement.model;

public class ReportData {
    // Results of reading data to TS tables
    DatastoreOutcome tieredStorageOutcome;
    DatastoreOutcome cachedStorageOutcome;

    public ReportData(){
        tieredStorageOutcome = new DatastoreOutcome();
        cachedStorageOutcome = new DatastoreOutcome();
    }

    public void addTsTable(String typeName, TableOutcome tableForDoc1) {
        tieredStorageOutcome.outcomeMap.put(typeName, tableForDoc1);
    }

    public void addCachedTable(String typeName, TableOutcome tableForDoc2) {
        cachedStorageOutcome.outcomeMap.put(typeName, tableForDoc2);
    }

    public TableOutcome getTsResults(String typeName) {
        return tieredStorageOutcome.outcomeMap.get(typeName);
    }

    public TableOutcome getCsResults(String typeName) {
        return cachedStorageOutcome.outcomeMap.get(typeName);
    }
}
