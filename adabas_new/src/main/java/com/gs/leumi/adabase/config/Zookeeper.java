package com.gs.leumi.adabase.config;

public class Zookeeper {
    private String address;
    private int port;
    private String connectionStr;
    private String path;

    public Zookeeper() {
        port = 2181;
        path = "/adabas/leader";
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getConnectionStr() {
        return connectionStr;
    }

    public void setConnectionStr(String connectionStr) {
        this.connectionStr = connectionStr;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
