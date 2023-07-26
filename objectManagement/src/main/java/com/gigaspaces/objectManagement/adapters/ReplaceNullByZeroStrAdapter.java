package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;


public class ReplaceNullByZeroStrAdapter extends PropertyStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SpreadingSecondLetterAdapter.class);

    public static void main(String[] args){
        ReplaceNullByZeroStrAdapter replaceNullByZeroStrAdapter = new ReplaceNullByZeroStrAdapter();

        try {
            System.out.println(replaceNullByZeroStrAdapter.replaceTest(null));
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public ReplaceNullByZeroStrAdapter() {
        super();
    }

    @Override
    public boolean supportsEqualsMatching() {
        return true;
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        try {
            return replace(value);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            logger.error("Failed to replace Object [ "  + value + " ] with '0'");
        }

        return value;
    }

    @Override
    public Object fromSpace(Object value) {
        return value.toString();
    }

    protected Object replace(Object object) throws SQLException {
        if (object == null)
            return '0';
        else
            return object;
    }

    protected Object replaceTest(Object value) throws SQLException {
        return replace(value);
    }
}
