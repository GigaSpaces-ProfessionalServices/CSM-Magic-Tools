package com.gigaspaces.objectManagement.utils;

import com.gigaspaces.objectManagement.service.DdlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdNoObfuscator {

    private Logger logger = LoggerFactory.getLogger(IdNoObfuscator.class);

    Integer pnimi;

    public IdNoObfuscator(Integer pnimi) {
        this.pnimi = pnimi;
    }

    public static void main(String[] args) {
        IdNoObfuscator idNoObfuscator = new IdNoObfuscator(12345);
        String newIdNo1 = idNoObfuscator.obfuscatToPnimi9Digits();
        System.out.println(newIdNo1);
    }

    public String obfuscatToPnimi9Digits() {
        try {
            // PNIMI is Integer and IDNO is a String
            String newIdNo = String.valueOf(pnimi);
            int pnimOrigLength = newIdNo.length();
            int addedNinesLength = 0;

            for (int i = 0; (addedNinesLength + pnimOrigLength) <= 8; i++) {
                newIdNo = "0".concat(newIdNo);
                addedNinesLength++;
            }

            return newIdNo;
        } catch (Throwable e) {
            logger.error("Failed to obfuscate. PNIMI: ", e);

            return null;
        }
    }

    public String obfuscatToPnimi14Digits() {
        try {
            // PNIMI is Integer and PASSPORT is a String
            String newPassport = String.valueOf(pnimi);
            int pnimOrigLength = newPassport.length();
            int addedNinesLength = 0;

            for (int i = 0; (addedNinesLength + pnimOrigLength) <= 12; i++) {
                newPassport = "0".concat(newPassport);
                addedNinesLength++;
            }

            return newPassport.concat("P");
        } catch (Throwable e) {
            logger.error("Failed to obfuscate passport to pnimi", e);

            return null;
        }
    }
}
