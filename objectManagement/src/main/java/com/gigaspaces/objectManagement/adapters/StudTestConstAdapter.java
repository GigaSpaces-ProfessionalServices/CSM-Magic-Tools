package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;

import java.io.IOException;

public class StudTestConstAdapter extends PropertyStorageAdapter {

    public static void main(String[] args){
        StudTestConstAdapter spreadingSecondLetterAdapter = new StudTestConstAdapter();
        System.out.println(spreadingSecondLetterAdapter.replaceByStudTestConstTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return replaceByStudTestConst((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object replaceByStudTestConst(String origName) { return "stud_test"; }

    protected Object replaceByStudTestConstTest(String data) {
        return "stud_test";
    }
}
