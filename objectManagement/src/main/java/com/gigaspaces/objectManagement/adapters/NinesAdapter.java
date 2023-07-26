package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;

import java.io.IOException;

public class NinesAdapter extends PropertyStorageAdapter {

    public static void main(String[] args){
        NinesAdapter spreadingSecondLetterAdapter = new NinesAdapter();
        System.out.println(spreadingSecondLetterAdapter.replaceByNinesTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return replaceByNines((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object replaceByNines(String origName) {
        return "999999999";
    }

    protected Object replaceByNinesTest(String data) {
        return "999999999";
    }
}
