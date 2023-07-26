package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;


public class OracleTimeStampToSqlTimeStampAdapter extends PropertyStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SpreadingSecondLetterAdapter.class);

    public static void main(String[] args){
        OracleTimeStampToSqlTimeStampAdapter oracleTimeStampToSqlTimeStampAdapter = new OracleTimeStampToSqlTimeStampAdapter();

        try {
            System.out.println(oracleTimeStampToSqlTimeStampAdapter.convertTest(new oracle.sql.TIMESTAMP("2023-08-12 15:20:30"))); // yyyy-MM-dd HH:mm:ss
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public Object toSpace(Object value) throws IOException {

        try {
            if (value != null && value instanceof oracle.sql.TIMESTAMP)
                return convert(value);
            else
                return value;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            logger.error("Failed to convert oracle.sql.TIMESTAMP" + value + " to java.sql.Timestamp. going to return null instead");
        }

        return null;
    }

    @Override
    public Object fromSpace(Object value) {
        return value;
    }

    protected Object convert(Object timestamp) throws SQLException {

        boolean isOracle = timestamp != null && timestamp instanceof oracle.sql.TIMESTAMP;
        logger.debug("in convert() - isOracle: " + isOracle);

        if (isOracle)
            return ((oracle.sql.TIMESTAMP)timestamp).timestampValue();
        else
            return timestamp;
    }

    protected Object convertTest(Object timestamp) throws SQLException {
        return convert(timestamp);
    }

    @Override
    public boolean supportsEqualsMatching() {
        return true;
    }
}
