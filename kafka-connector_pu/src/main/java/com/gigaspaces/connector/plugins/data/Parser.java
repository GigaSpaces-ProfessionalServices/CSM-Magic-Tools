package com.gigaspaces.connector.plugins.data;

public interface Parser {
    void parse(Object message);

    boolean containsValue(String valueSelector);

    Object getValue(String valueSelector);
}
