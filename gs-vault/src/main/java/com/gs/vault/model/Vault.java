package com.gs.vault.model;

public class Vault {
    private int id;
    private String propertyName;
    private String encryptedValue;

    public Vault(String propertyName, String encryptedValue) {
        this.propertyName = propertyName;
        this.encryptedValue = encryptedValue;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }
}
