package com.gigaspaces.connector.plugins.data;

import com.gigaspaces.connector.data.DataException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonParser implements Parser {

    private DocumentContext parsedJson;

    @Override
    public void parse(Object message) {
        parsedJson = JsonPath.parse((String) message);
        boolean isStringAfterParsing = parsedJson.json() instanceof String;
        if (isStringAfterParsing) throw new DataException("Could not parse message as JSON:\n{}", message);
    }

    @Override
    public boolean containsValue(String valueSelector) {
        Object value;
        try {
            value = parsedJson.read(valueSelector);
        } catch (PathNotFoundException e) {
            return false;
        }

        if (value instanceof JSONArray
                && ((JSONArray) value).size() == 0
                && isArrayElementSelector(valueSelector)) {
            return false;
        }
        return true;
    }

    @Override
    public Object getValue(String valueSelector) {
        Object value = null;
        try {
            value = parsedJson.read(valueSelector);
        } catch (PathNotFoundException e) {
            throw new DataException("Path '{}' not found.", valueSelector);
        }

        if (isArrayElementSelector(valueSelector)) {
            if (value instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) value;
                if (jsonArray.size() == 0) {
                    throw new DataException("No results for '{}' selector", valueSelector);
                }

                if (jsonArray.size() > 1) {
                    throw new DataException("Got multiple values while expecting single value for selector '{}'",
                            valueSelector);
                }

                return jsonArray.get(0);
            }
        }
        return value;
    }

    // This is a workaround to the issue where for some reason JSON parser returns single value array
    // where we expect a primitive value
    // see also https://stackoverflow.com/questions/23608050/getting-a-single-value-from-a-json-object-using-jsonpath
    private boolean isArrayElementSelector(String valueSelector) {
        // <something> [? <something> ] <something>
        String patternString = ".*\\[\\?.*\\].*";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(valueSelector);
        return matcher.matches();
    }
}