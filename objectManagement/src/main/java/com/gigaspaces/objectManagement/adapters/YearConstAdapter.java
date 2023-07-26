package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;

import java.io.IOException;

public class YearConstAdapter extends PropertyStorageAdapter {

    public static void main(String[] args){
        YearConstAdapter spreadingSecondLetterAdapter = new YearConstAdapter();
        System.out.println(spreadingSecondLetterAdapter.replaceByYearConstTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return replaceByYearConst((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object replaceByYearConst(String origName) { return "20000101"; }

    protected Object replaceByYearConstTest(String data) {
        return "20000101";
    }
}
