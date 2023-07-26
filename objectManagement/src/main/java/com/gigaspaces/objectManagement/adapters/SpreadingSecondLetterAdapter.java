package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpreadingSecondLetterAdapter extends PropertyStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SpreadingSecondLetterAdapter.class);

    public static void main(String[] args){
        SpreadingSecondLetterAdapter spreadingSecondLetterAdapter = new SpreadingSecondLetterAdapter();
        System.out.println(spreadingSecondLetterAdapter.spreadTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return spread((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object spread(String origName) {

        if (origName == null)
            return "";

        if (origName.length() == 0 || origName.length() == 1 || origName.length() == 2)
            return origName;

        String firstLetter = origName.substring(0,1);
        String secondLetter = origName.substring(1,2);

        int length = origName.length();
        StringBuffer spreadName = new StringBuffer(110);
        spreadName.append(firstLetter);

        for (int idx=1; idx<length; idx++){
            spreadName.append(secondLetter);
        }

        return spreadName.toString();
    }

    protected Object spreadTest(String origName) {

        if (origName == null)
            return "";

        if (origName.length() == 1 || origName.length() == 2)
            return origName;

        String firstLetter = origName.substring(0,1);
        String secondLetter = origName.substring(1,2);

        System.out.println(" ##### origName = " + origName + " #####");
        System.out.println(" ##### firstLetter = " + firstLetter + " #####");
        System.out.println(" ##### secondLetter = " + secondLetter + " #####");

        int length = origName.length();
        StringBuffer spreadName = new StringBuffer(110);
        spreadName.append(firstLetter);

        for (int idx=1; idx<length; idx++){
            spreadName.append(secondLetter);
        }

        return spreadName.toString();
    }



}
