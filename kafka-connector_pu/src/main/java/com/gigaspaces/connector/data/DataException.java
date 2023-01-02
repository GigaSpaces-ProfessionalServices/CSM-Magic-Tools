package com.gigaspaces.connector.data;

import org.slf4j.helpers.MessageFormatter;

public class DataException extends RuntimeException {
    public DataException(String string, Object... objects) {
        super(MessageFormatter.arrayFormat(string, objects).getMessage());
    }
}
