package com.gigaspaces.objectManagement.model;

public class IndexDetail {
    String tableName;
    String columnName;
    String indexType;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    @Override
    public String toString() {
        return "IndexDetail{" +
                "tableName='" + tableName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", indexType='" + indexType + '\'' +
                '}';
    }
}
