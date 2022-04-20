package com.odsx.services.catalogueservice.response;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class MetadataResponse {

    private String tableName;
    private List<String> serviceList;
    private String errorMsg;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<String> serviceList) {
        this.serviceList = serviceList;
    }

    public String getErrMsg() {
        return errorMsg;
    }

    public void setErrMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataResponse that = (MetadataResponse) o;
        return tableName.equals(that.tableName) && Objects.equals(serviceList, that.serviceList) && Objects.equals(errorMsg, that.errorMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, serviceList, errorMsg);
    }

    @Override
    public String toString() {
        return "MetadataResponse{" +
                "tableName='" + tableName + '\'' +
                ", serviceList=" + serviceList +
                ", errMsg='" + errorMsg + '\'' +
                '}';
    }
}
