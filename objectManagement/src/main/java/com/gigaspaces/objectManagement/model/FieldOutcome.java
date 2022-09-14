package com.gigaspaces.objectManagement.model;

import java.util.Map;
import java.util.TreeMap;

public class FieldOutcome {

    public String fieldName;
    public String fieldDataType;
    public String value;
    public boolean couldWrite;
    public boolean couldRead;

    Map<String, Object> additionalInfo;

    public FieldOutcome(){
        additionalInfo = new TreeMap<>();
    }


}
