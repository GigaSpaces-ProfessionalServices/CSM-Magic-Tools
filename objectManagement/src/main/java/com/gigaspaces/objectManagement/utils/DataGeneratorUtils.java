package com.gigaspaces.objectManagement.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.util.Random;

public class DataGeneratorUtils {
    private static final Random random = new Random();

    public static String singleDigitSafeRand() {
        return String.valueOf(Math.abs(random.nextInt(10)));
    }

    public static String random(int numberOfDigits) {
        int sign = random.nextInt();
        String rand = positiveRandom(numberOfDigits);
        if (sign < 0) {
            return "-" + rand;
        } else {
            return rand;
        }
    }

    public static String positiveRandom(int numberOfDigits) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numberOfDigits; i++) {
            result.append(singleDigitSafeRand());
        }
        return result.toString();
    }

    //precision - maximum number of digits (the precision)
    //scale - number of digits to the right of the decimal point (the scale)
    //if p is precision; s is scale. then 0<=s<=p<=63
    public static String doubleSafeRand(int precision, int scale) {
        return positiveRandom(precision - scale) + "." + positiveRandom(scale);
    }

    public static Date randomDate(long base) {
        // return new Date(base - Long.parseLong(positiveRandom(12)));
        return new Date(base - Long.parseLong(positiveRandom(10)));
    }


    public static Object getPropertyValue(int i, Class<?> typeName) {
        if (String.class.equals(typeName)) {
            return "V" + i;
        } else if (Integer.class.equals(typeName)) {
            return Integer.parseInt(positiveRandom(4));
        } else if (BigInteger.class.equals(typeName)) {
            return new BigInteger(positiveRandom(7));
        } else if (Short.class.equals(typeName)) {
            return Short.parseShort(positiveRandom(4));
        } else if (Double.class.equals(typeName)) {
            return Double.parseDouble(doubleSafeRand(8, 5));
        } else if (Float.class.equals(typeName)) {
            return Float.parseFloat(doubleSafeRand(6, 4));
        } else if (BigDecimal.class.equals(typeName)) {
            // Format - precision e.g. 5.2
            return new BigDecimal(Integer.MAX_VALUE).add(new BigDecimal(doubleSafeRand(10, random.nextInt(5))));
        } else if (Date.class.equals(typeName)) {
            return new Date(randomDate(Integer.parseInt(positiveRandom(4))).getTime());
        } else if (java.sql.Timestamp.class.equals(typeName)) {
            return new java.sql.Timestamp(System.currentTimeMillis() + Integer.parseInt(positiveRandom(4)));
        } else if (java.sql.Time.class.equals(typeName)) {
            return new java.sql.Time(System.currentTimeMillis() + Integer.parseInt(positiveRandom(4)));
        } else if (Boolean.class.equals(typeName)) {
            if (i == 0) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        } else {
            //Default to String
            return "UNKNOWN_TYPE-" + i;
        }
    }

}
