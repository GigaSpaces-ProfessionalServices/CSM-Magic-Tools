package com.gs.leumi.common.model;

import java.io.Serializable;
import java.util.Objects;

public class Envalope implements Serializable {
    private String type;
    private String operation;  //CREATE, INSERT, UPDATE, DELETE
    private Object payload;

    public Envalope(){}

    public Envalope(String type, String operation, Object payload) {
        this.type = type;
        this.operation = operation;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Envalope envalope = (Envalope) o;
        return Objects.equals(type, envalope.type) &&
                Objects.equals(operation, envalope.operation) &&
                Objects.equals(payload, envalope.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, operation, payload);
    }
}
