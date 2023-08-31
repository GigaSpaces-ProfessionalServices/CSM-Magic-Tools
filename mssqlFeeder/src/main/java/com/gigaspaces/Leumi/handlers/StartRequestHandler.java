package com.gigaspaces.Leumi.handlers;

import com.gigaspaces.Leumi.dto.FeederConfig;
import com.gigaspaces.Leumi.dto.InputParams;
import com.gigaspaces.Leumi.dto.MSSQLConfig;
import com.gigaspaces.Leumi.utils.Progress;
import com.gigaspaces.Leumi.utils.SqlStatementBulder;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.extensions.QueryExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Route;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;


public class StartRequestHandler extends BaseRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(StartRequestHandler.class);
    private final MSSQLConfig mssqlConfig;
    private final FeederConfig feederConfig;
    private final Progress progress;
    private Connection connection = null;
    private static boolean stopped = false;

    public enum Status {IDLE, IN_PROGRESS, ERROR, SUCCESS}

    static Status status = Status.IDLE;

    public StartRequestHandler(GigaSpace space, MSSQLConfig mssqlConfig, FeederConfig feederConfig, Progress progress) {
        super(space);
        this.mssqlConfig = mssqlConfig;
        this.feederConfig = feederConfig;
        this.progress = progress;
    }

    public Route process = (request, response) -> {
        if (status.equals(StartRequestHandler.Status.IN_PROGRESS)) {
            response.status(503);
            return "Already in progress";
        }
        logger.info("processing start request");
        stopped = false;
        InputParams inputParams = getInputParams(request);

        startFeederThread(inputParams);

        return "OK";
    };

    private void startFeederThread(InputParams inputParams) {

        FeederThread feederThread = new FeederThread();
        feederThread.inputParams = inputParams;
        ExecutorService executorService =
                new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());

        executorService.execute(feederThread);
    }

    private InputParams getInputParams(Request request) {
        String tableName = request.queryParamOrDefault("table-name", null);
        String schemaName = request.queryParamOrDefault("schema-name", null);
        String baseColumn = request.queryParamOrDefault("base-column", null);
        logger.info("base-column is : "+baseColumn);
        String rowsLimit = request.queryParamOrDefault("rows-limit", "0");
        long limit = Long.parseLong(rowsLimit);
        String excludeColumns = request.queryParamOrDefault("exclude-columns", null);
        String pkColumns = request.queryParamOrDefault("pk-columns", null);
        String baseColumnExpression = request.queryParamOrDefault("base-column-expression", null);
        String condition = request.queryParamOrDefault("condition", null);

        InputParams inputParams = new InputParams(
                tableName,
                schemaName,
                baseColumn,
                limit,
                excludeColumns,
                pkColumns,
                baseColumnExpression,
                condition
        );

        validateMandatoryParameters(inputParams);

        return inputParams;
    }

    private void validateMandatoryParameters(InputParams inputParams) {
        if (inputParams.getTableName() == null) {
            logger.error("Missing 'table-name' query parameter");
            throw new RuntimeException();
        }

        if (inputParams.getBaseColumn() == null) {
            logger.error("Missing 'base-column' query parameter");
            throw new RuntimeException();
        }
    }

    class FeederThread implements Runnable {
        InputParams inputParams;

        @Override
        public void run() {
            status = Status.IN_PROGRESS;
            try {
                feedTableData(inputParams);
                status = Status.SUCCESS;
            } catch (Exception e) {
                status = Status.ERROR;
                logger.error(e.toString());
                logger.error(e.getStackTrace().toString());
            }
            logger.info("Status {}", status);
        }
    }

    private void feedTableData(InputParams inputParams) throws SQLException, ClassNotFoundException, InterruptedException {
        SpaceTypeDescriptor typeDescriptor = space.getTypeManager().getTypeDescriptor(inputParams.getTableName());
        if (typeDescriptor == null) {
            logger.error("Type '{}' not found in space.", inputParams.getTableName());
            throw new RuntimeException();
        }

        logger.info("Connecting to MSSQL...");

        connectToMSSQL();

        logger.info("Connected to MSSQL successfully");

        Object maxBaseValueInSpace = getMaxBaseValueInSpace(typeDescriptor.getTypeName(), inputParams);
        logger.info("Max {}.{} value in space is {}", inputParams.getTableName(), inputParams.getBaseColumn(), maxBaseValueInSpace);

        logger.info("Calculating record count to be transferred...");
        int count = getRecordsCount(inputParams, maxBaseValueInSpace, typeDescriptor.getIdPropertyName());
        logger.info("Count={}", count);
        progress.startOver(count);

        logger.info("Executing query...");
        ResultSet rs = executeQuery(typeDescriptor, inputParams, maxBaseValueInSpace);
        logger.info("Query executed successfully");

        logger.info("Starting to transfer data...");
        writeDataToSpace(typeDescriptor, inputParams, rs);
        logger.info("Finished to transfer data!");

        logger.info("Going to close the connection");
        connection.close();
        logger.info("Connection closed");
    }

    private void writeDataToSpace(SpaceTypeDescriptor typeDescriptor, InputParams inputParams, ResultSet rs) throws SQLException, InterruptedException {
        String[] propertiesNames = SqlStatementBulder.getPropertiesWithoutExcluded(typeDescriptor.getPropertiesNames(), inputParams.getExcludeColumns());
        ArrayList<SpaceDocument> batch = new ArrayList<>();
        while (rs.next() && !stopped) {
            SpaceDocument spaceDocument = new SpaceDocument(typeDescriptor.getTypeName());

            if (inputParams.getPkColumns() != null) {
                String idValue = getConcatenatedPkValue(rs, inputParams.getPkColumns());
                spaceDocument.setProperty(typeDescriptor.getIdPropertyName(), idValue);
                // pk-columns param is not provided when type has autogenerated id
            }

            for (String prop : propertiesNames) {
                // space ID property does not have column in source DB
                if (prop.equals(typeDescriptor.getIdPropertyName()))
                    continue;

                Object value;
                try {
                    // convert sql.Date to util.Date if needed
                    if (typeDescriptor.getFixedProperty(prop).getType().equals(Date.class)) {
                        value = rs.getTimestamp(prop);
                    } else if (typeDescriptor.getFixedProperty(prop).getType().equals(Short.class)) {
                        value = rs.getShort(prop);
                    } else {
                        value = rs.getObject(prop);
                    }
                    spaceDocument.setProperty(prop, value);
                } catch (Exception e) {
                    logger.error("Conversion error. Property '{}.{}', type in space '{}'.",
                            typeDescriptor.getTypeName(),
                            prop,
                            typeDescriptor.getFixedProperty(prop).getTypeName());
                    try {
                        logger.error("JDBC type: '{}'", rs.getObject(prop).getClass().getName());
                    } catch (SQLException ex) {
                        logger.error("Unable to print JDBC type of the column.");
                    }
                    throw e;
                }
            }
            batch.add(spaceDocument);

            if (batch.size() == feederConfig.getWriteBatchSize()) {
                space.writeMultiple(batch.toArray(new SpaceDocument[0]));
                progress.increment(batch.size());
                batch.clear();
                Thread.sleep(feederConfig.getSleepAfterWriteInMillis());
            }
            progress.printProgress();
        }

        if (batch.size() > 0) {
            space.writeMultiple(batch.toArray(new SpaceDocument[0]));
            progress.increment(batch.size());
            batch.clear();
        }
        progress.printProgressUnconditionally();
    }

    private String getConcatenatedPkValue(ResultSet rs, String pkColumns) throws SQLException {
        String[] pkColumnsArr = pkColumns.split(",");
        ArrayList<String> pkValues = new ArrayList<>();
        for (String pkColumn : pkColumnsArr) {
            try {
                pkValues.add(rs.getString(pkColumn));
            } catch (SQLException e) {
                logger.error("rs.getString('{}') failed", pkColumn);
                throw e;
            }
        }
        return String.join("|", pkValues);
    }

    private ResultSet executeQuery(SpaceTypeDescriptor typeDescriptor, InputParams inputParams, Object maxBaseValueInSpace) throws SQLException {
        String sql = SqlStatementBulder.buildSelectDataSql(inputParams, typeDescriptor, maxBaseValueInSpace);
        logger.info(sql);

        PreparedStatement preparedStatement = getPreparedStatement(sql, maxBaseValueInSpace);

        ResultSet rs = preparedStatement.executeQuery();
        return rs;
    }

    private PreparedStatement getPreparedStatement(String sql, Object maxBaseValueInSpace) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        if (maxBaseValueInSpace == null) return preparedStatement;

        if (maxBaseValueInSpace instanceof java.util.Date) {
            java.sql.Date date = new java.sql.Date(((Date) maxBaseValueInSpace).getTime());
            preparedStatement.setDate(1, date);
        } else {
            preparedStatement.setObject(1, maxBaseValueInSpace);
        }

        return preparedStatement;
    }

    private int getRecordsCount(InputParams inputParams, Object maxBaseValueInSpace, String idPropertyName) throws SQLException {
        String SQL = SqlStatementBulder.buildCountSql(inputParams, maxBaseValueInSpace);
        logger.info(SQL);
        PreparedStatement preparedStatement = getPreparedStatement(SQL, maxBaseValueInSpace);
        ResultSet rs = preparedStatement.executeQuery();
        rs.next();
        int count = rs.getInt(1);

        return count;
    }

    private void connectToMSSQL() throws ClassNotFoundException, SQLException {
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName(this.mssqlConfig.getServer());
        ds.setIntegratedSecurity(this.mssqlConfig.getIntegratedSecurity());
        ds.setAuthenticationScheme(this.mssqlConfig.getAuthenticationScheme());
        ds.setDatabaseName(this.mssqlConfig.getDatabaseName());

        connection = ds.getConnection();
    }

    private Object getMaxBaseValueInSpace(String typeName, InputParams inputParams) {
        SQLQuery<SpaceDocument> spaceDocumentSQLQuery = new SQLQuery<>(typeName, "");
        String convertedBasePropertyName = SqlStatementBulder.getConvertedBasePropertyName(inputParams);

        int count = space.count(spaceDocumentSQLQuery);
        if (count == 0) return null;

        Comparable max = null;

        try {
            max = QueryExtension.max(space, spaceDocumentSQLQuery, convertedBasePropertyName);
        }catch (IllegalArgumentException e){
            logger.info("Table " + inputParams.getTableName() + " doesn't contain a condition");
        }

        return max;
    }
    public static void stop() {
        stopped = true;
    }
}
