package com.gigaspaces.connector.config;

import org.slf4j.helpers.MessageFormatter;

public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String string, Object... objects) {
        super(MessageFormatter.arrayFormat(string, objects).getMessage());
    }
}
