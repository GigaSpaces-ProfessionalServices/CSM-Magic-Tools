package com.gigaspaces.connector.data;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.document.DocumentProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class ValueToSpaceTypeConverter {

    public Object convert(Object value, DataPipelineConfig.Property property) {
        // TODO: 14/12/2021 convert list values to given type?
        if (value instanceof List)
            return convertToSimpleTypes((List) value);

        String propType = property.getType();

        if (propType.equals(Object.class.getName()) || propType.equals(Object.class.getSimpleName()))
            return value;

        Map<String, Function<String, Object>> parsers = getParsers();

        Function<String, Object> typeParser = parsers.get(propType);
        if (typeParser == null) typeParser = parsers.get("java.lang." + propType);

        if (typeParser == null) throw new ConfigurationException("Unknown Gigaspaces property type '{}'", propType);

        // TODO: 19/11/2021 in some cases simple casting may be more efficient (example Integer to Long)
        String stringSrcValue = PrimitiveValueToStringConverter.convert(value);
        Object spaceValue;

        if (stringSrcValue == null) {
            spaceValue = null;
        } else {
            try {
                spaceValue = typeParser.apply(stringSrcValue);
            } catch (Exception e) {
                throw new DataException("Unable to convert value '{}' to type '{}'", stringSrcValue, propType);
            }
        }
        return spaceValue;
    }

    private List convertToSimpleTypes(List value) {
        if (value.getClass() == ArrayList.class)
            return value;
        ArrayList<Object> objects = new ArrayList<>();
        for (Object o : value) {
            if (o instanceof Map)
                objects.add(convertToSimpleTypes((Map) o));
            else if (o instanceof List)
                objects.add(convertToSimpleTypes((List) o));
            else
                objects.add(o);
        }
        return objects;
    }

    private Object convertToSimpleTypes(Map<String, Object> map) {
        DocumentProperties documentProperties = new DocumentProperties();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                documentProperties.setProperty(entry.getKey(), convertToSimpleTypes((Map<String, Object>) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                documentProperties.setProperty(entry.getKey(), convertToSimpleTypes((List) entry.getValue()));
            } else {
                documentProperties.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return documentProperties;
    }

    private BigDecimal parseBigDecimal(String str) {
        return new BigDecimal(str);
    }

    private Map<String, Function<String, Object>> getParsers() {
        Map<String, Function<String, Object>> result = new HashMap<>();

        result.put(String.class.getName(), s -> s);
        result.put(boolean.class.getName(), Boolean::parseBoolean);
        result.put(Boolean.class.getName(), Boolean::parseBoolean);
        result.put(byte.class.getName(), Byte::parseByte);
        result.put(Byte.class.getName(), Byte::parseByte);
        result.put(short.class.getName(), Short::parseShort);
        result.put(Short.class.getName(), Short::parseShort);
        result.put(int.class.getName(), Integer::parseInt);
        result.put(Integer.class.getName(), Integer::parseInt);
        result.put(long.class.getName(), Long::parseLong);
        result.put(Long.class.getName(), Long::parseLong);
        result.put(float.class.getName(), Float::parseFloat);
        result.put(Float.class.getName(), Float::parseFloat);
        result.put(double.class.getName(), Double::parseDouble);
        result.put(Double.class.getName(), Double::parseDouble);
        result.put(BigDecimal.class.getName(), this::parseBigDecimal);
        result.put(char.class.getName(), s -> s.charAt(0));
        result.put(Character.class.getName(), s -> s.charAt(0));
        result.put(java.time.LocalDate.class.getName(), java.time.LocalDate::parse);
        result.put(java.time.LocalTime.class.getName(), java.time.LocalTime::parse);
        result.put(java.time.LocalDateTime.class.getName(), java.time.LocalDateTime::parse);
        result.put("string", result.get(String.class.getName()));
        result.put("date", result.get(java.time.LocalDate.class.getName()));
        result.put("Date", result.get(java.time.LocalDate.class.getName()));
        result.put("time", result.get(java.time.LocalTime.class.getName()));
        result.put("datetime", result.get(java.time.LocalDateTime.class.getName()));

        return result;
    }

    private boolean isList(DataPipelineConfig.Property property) {
        return property.getAttributes() != null && property.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.list);
    }

}
