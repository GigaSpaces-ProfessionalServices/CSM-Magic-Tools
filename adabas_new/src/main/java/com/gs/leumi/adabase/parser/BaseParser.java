package com.gs.leumi.adabase.parser;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class  BaseParser {
    private final Map<String, Parser> parsers;
    public final Map<String, String> nameTypeMap;
    public final String tableName;
    private static final String DECIMAL_FORMAT = "#,##0.0#";

    public BaseParser(){
        this(new Builder());
    }

    public BaseParser(Builder builder){
        this.parsers = builder.parsers;
        this.nameTypeMap = builder.nameTypeMap;
        this.tableName = builder.tableName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder <T extends Builder<T>>{
        private final Map<String, Parser> parsers = initDefaultParsers();
        private final Map<String, String> nameTypeMap = new HashMap<>();
        private  String tableName;
        public BaseParser build(){
            return new BaseParser();
        }

        public T addField(String name, String type){
            nameTypeMap.put(name, type);
            return (T)this;
        }
        public T tableName(String name){
            tableName = name;
            return (T)this;
        }
    }

    public Parser getParser(String type){
        return parsers.get(type);
    }

    public static class Parser {
        public final Class<?> type;
        public final Function<String, Object> parser;

        private Parser(Class<?> type, Function<String, Object> parser) {
            this.type = type;
            this.parser = parser;
        }
    };

    private static Map<String, Parser> initDefaultParsers() {
        Map<String, Parser> result = new HashMap<>();

        result.put(String.class.getName(), new Parser(String.class, s -> s));
        result.put(boolean.class.getName(), new Parser(boolean.class, Boolean::parseBoolean));
        result.put(Boolean.class.getName(), new Parser(Boolean.class, Boolean::parseBoolean));
        result.put(byte.class.getName(), new Parser(byte.class, Byte::parseByte));
        result.put(Byte.class.getName(), new Parser(Byte.class, Byte::parseByte));
        result.put(short.class.getName(), new Parser(short.class, Short::parseShort));
        result.put(Short.class.getName(), new Parser(Short.class, Short::parseShort));
        result.put(int.class.getName(), new Parser(int.class, Integer::parseInt));
        result.put(Integer.class.getName(), new Parser(Integer.class, Integer::parseInt));
        result.put(long.class.getName(), new Parser(long.class, Long::parseLong));
        result.put(Long.class.getName(), new Parser(Long.class, Long::parseLong));
        result.put(float.class.getName(), new Parser(float.class, Float::parseFloat));
        result.put(Float.class.getName(), new Parser(Float.class, Float::parseFloat));
        result.put(double.class.getName(), new Parser(double.class, Double::parseDouble));
        result.put(Double.class.getName(), new Parser(Double.class, Double::parseDouble));
        result.put(char.class.getName(), new Parser(char.class, s -> s.charAt(0)));
        result.put(BigDecimal.class.getName(), new Parser(BigDecimal.class, s -> {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator(',');
            symbols.setDecimalSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_FORMAT, symbols);
            decimalFormat.setParseBigDecimal(true);
            try {
                return (BigDecimal)decimalFormat.parse(s);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }));
        result.put(Character.class.getName(), new Parser(Character.class, s -> s.charAt(0)));
        result.put(java.time.LocalDate.class.getName(), new Parser(java.time.LocalDate.class, java.time.LocalDate::parse));
        result.put(java.time.LocalTime.class.getName(), new Parser(java.time.LocalTime.class, java.time.LocalTime::parse));
        result.put(java.time.LocalDateTime.class.getName(), new Parser(java.time.LocalDateTime.class, java.time.LocalDateTime::parse));
        result.put("string", result.get(String.class.getName()));
        result.put("date", result.get(java.time.LocalDate.class.getName()));
        result.put("time", result.get(java.time.LocalTime.class.getName()));
        result.put("datetime", result.get(java.time.LocalDateTime.class.getName()));

        return result;
    }

}
