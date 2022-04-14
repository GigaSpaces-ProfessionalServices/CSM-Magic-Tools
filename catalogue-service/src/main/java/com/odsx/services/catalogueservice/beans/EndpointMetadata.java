package com.odsx.services.catalogueservice.beans;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class EndpointMetadata {

    private String endpointName;

    private Integer numberOfInstances;

    private List<String> metadata;

    private String errorMsg;

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<String> metadata) {
        this.metadata = metadata;
    }

    public Integer getNumberOfInstances() {
        return numberOfInstances;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    @Override
    public String toString() {
        return "EndpointMetadata{" +
                "endpointName='" + endpointName + '\'' +
                ", numberOfInstances=" + numberOfInstances +
                ", metadata=" + metadata +
                ", errorMsg='" + errorMsg + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointMetadata that = (EndpointMetadata) o;
        return endpointName.equals(that.endpointName) && Objects.equals(numberOfInstances, that.numberOfInstances) && Objects.equals(metadata, that.metadata) && Objects.equals(errorMsg, that.errorMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointName, numberOfInstances, metadata, errorMsg);
    }
}
