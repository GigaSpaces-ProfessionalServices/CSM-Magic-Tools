package com.gigaspaces.Leumi.utils;

import com.gigaspaces.Leumi.dto.InputParams;
import com.gigaspaces.metadata.SpaceTypeDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SqlStatementBulder {

    public static String buildSelectDataSql(InputParams inputParams, SpaceTypeDescriptor typeDescriptor, Object maxBaseValueInSpace) {
        String[] selectColumns = getSelectColumns(inputParams, typeDescriptor);

        String commaSeparatedColumnNamesList = String.join(",", selectColumns);

        String SQL = getSql(inputParams, maxBaseValueInSpace, commaSeparatedColumnNamesList, false, false, false);

        return SQL;
    }

    private static String[] getSelectColumns(InputParams inputParams, SpaceTypeDescriptor typeDescriptor) {
        String[] propertiesNames = typeDescriptor.getPropertiesNames();
        propertiesNames = getPropertiesWithoutExcluded(propertiesNames, inputParams.getExcludeColumns());
        propertiesNames = subtractArrayFromArray(propertiesNames, new String[]{typeDescriptor.getIdPropertyName()});
        if (inputParams.getBaseColumnExpression() != null)
            propertiesNames = subtractArrayFromArray(propertiesNames, new String[]{inputParams.getBaseColumn() + "_CNV"});
        return propertiesNames;
    }

    public static String buildCountSql(InputParams inputParams, Object maxBaseValueInSpace) {
        String subSelect = getSql(inputParams, maxBaseValueInSpace, inputParams.getBaseColumn(), false, false, false);

        String SQL = String.format("SELECT COUNT(*) FROM (%s) a ", subSelect);

        return SQL;
    }

    public static String[] getPropertiesWithoutExcluded(String[] propertiesNames, String excludedColumns) {
        if (excludedColumns != null) {
            String[] excludedColumnsArr = excludedColumns.split(",");
            propertiesNames = subtractArrayFromArray(propertiesNames, excludedColumnsArr);
        }
        return propertiesNames;
    }

    private static String getSql(InputParams inputParams, Object maxBaseValueInSpace, String columns,
                                 boolean addOrderClause, boolean addConvertedColumn, boolean addWith) {
        String schemaAndTable;
        if (inputParams.getSchemaName() == null) {
            schemaAndTable = inputParams.getTableName();
        } else {
            schemaAndTable = inputParams.getSchemaName() + '.' + inputParams.getTableName();
        }

        if (addConvertedColumn && inputParams.getBaseColumnExpression() != null)
            columns += String.format(",%s AS %s",
                    inputParams.getBaseColumnExpression(),
                    getConvertedBasePropertyName(inputParams));

        String SQL = String.format("SELECT %s FROM %s [nolock]", columns, schemaAndTable);

        String db2baseColumnExpression = inputParams.getBaseColumnExpression() == null ?
                inputParams.getBaseColumn() : inputParams.getBaseColumnExpression();

        List<String> whereConditions = new ArrayList<>();
        if (maxBaseValueInSpace != null)
            whereConditions.add(String.format("%s>=?", db2baseColumnExpression));

        if (inputParams.getCondition() != null)
            whereConditions.add(inputParams.getCondition());

        if (whereConditions.size() > 0) {
            String whereConditionsStr = String.join(" AND ", whereConditions);
            SQL += String.format(" WHERE %s", whereConditionsStr);
        }

        if (addOrderClause)
            SQL += String.format(" ORDER BY %s", db2baseColumnExpression);

        if (inputParams.getRowsLimit() > 0)
            SQL += String.format(" LIMIT %d", inputParams.getRowsLimit());

        if (addWith)
            SQL += " WITH UR";

        return SQL;

    }

    private static String[] subtractArrayFromArray(String[] arr1, String[] arr2) {
        List<String> list1 = new LinkedList<>(Arrays.asList(arr1));
        List<String> list2 = Arrays.asList(arr2);
        list1.removeAll(list2);
        return list1.toArray(new String[0]);
    }

    public static String getConvertedBasePropertyName(InputParams inputParams) {
        if (inputParams.getBaseColumnExpression() == null) {
            return inputParams.getBaseColumn();
        } else {
            return inputParams.getBaseColumn() + "_CNV";
        }
    }

}
