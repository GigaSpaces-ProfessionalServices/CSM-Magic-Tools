package com.gigaspaces.connector.data;

public class PrimitiveValueToStringConverter {

    public static String convert(Object parserValue) {
        String stringSrcValue;
        if (parserValue instanceof Integer ||
                parserValue instanceof Double ||
                parserValue instanceof Float ||
                parserValue instanceof Long ||
                parserValue instanceof Boolean)
            stringSrcValue = String.valueOf(parserValue);
        else
            stringSrcValue = (String) parserValue;
        return stringSrcValue;
    }
}
