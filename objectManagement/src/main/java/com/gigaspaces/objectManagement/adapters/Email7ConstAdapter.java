package com.gigaspaces.objectManagement.adapters;

import com.gigaspaces.client.storage_adapters.PropertyStorageAdapter;

import java.io.IOException;

public class Email7ConstAdapter extends PropertyStorageAdapter {

    public static void main(String[] args){
        Email7ConstAdapter spreadingSecondLetterAdapter = new Email7ConstAdapter();
        System.out.println(spreadingSecondLetterAdapter.replaceByMail7ConstTest("Yuval"));
    }

    @Override
    public Object toSpace(Object value) throws IOException {
        return replaceByMail7Const((String)value);
    }

    @Override
    public Object fromSpace(Object value) throws IOException, ClassNotFoundException {
        return value.toString();
    }

    protected Object replaceByMail7Const(String origName) { return "outsystemsdev@tauex.tau.ac.il"; }

    protected Object replaceByMail7ConstTest(String data) {
        return "outsystemsdev@tauex.tau.ac.il";
    }
}
