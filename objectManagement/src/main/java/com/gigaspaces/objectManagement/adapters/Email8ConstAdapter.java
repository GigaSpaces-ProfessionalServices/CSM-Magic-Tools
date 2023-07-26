package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;

import java.io.IOException;

public class Email8ConstAdapter extends PropertyStorageAdapter {

    public static void main(String[] args){
        Email8ConstAdapter spreadingSecondLetterAdapter = new Email8ConstAdapter();
        System.out.println(spreadingSecondLetterAdapter.replaceByMail8ConstTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return replaceByMail8Const((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object replaceByMail8Const(String origName) { return "outsystemsdev2@tauex.tau.ac.il"; }

    protected Object replaceByMail8ConstTest(String data) {
        return "outsystemsdev2@tauex.tau.ac.il";
    }
}
