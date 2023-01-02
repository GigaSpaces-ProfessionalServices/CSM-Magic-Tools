package com.gigaspaces.connector.cdc;

import org.slf4j.helpers.MessageFormatter;

public class CdcException extends RuntimeException {
    public CdcException(String string, Object... objects) {
        super(MessageFormatter.arrayFormat(string, objects).getMessage());
    }
}
