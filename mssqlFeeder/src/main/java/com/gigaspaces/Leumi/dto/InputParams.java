package com.gigaspaces.Leumi.dto;

public class InputParams {
    public InputParams(String tableName, String schemaName, String baseColumn, long rowsLimit, String excludeColumns, String pkColumns, String baseColumnExpression, String condition) {
        this.tableName = tableName;
        this.baseColumn = baseColumn;
        this.rowsLimit = rowsLimit;
        this.excludeColumns = excludeColumns;
        this.schemaName = schemaName;
        this.pkColumns = pkColumns;
        this.baseColumnExpression = baseColumnExpression;
        this.condition = condition;
    }

    private String tableName;
    private String schemaName;
    private String pkColumns;
    private String baseColumnExpression;
    private String condition;
    private String baseColumn;
    private long rowsLimit;
    private String excludeColumns;

    public String getTableName() {
        return tableName;
    }

    public String getBaseColumn() {
        return baseColumn;
    }

    public long getRowsLimit() {
        return rowsLimit;
    }

    public String getExcludeColumns() {
        return excludeColumns;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getPkColumns() {
        return pkColumns;
    }

    public void setExcludeColumns(String excludeColumns) {
        this.excludeColumns = excludeColumns;
    }

    public String getBaseColumnExpression() {
        return baseColumnExpression;
    }

    public String getCondition() {
        return condition;
    }
}
