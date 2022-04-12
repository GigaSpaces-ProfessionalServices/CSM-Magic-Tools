package com.odsx.services.catalogueservice.beans;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class EndpointMetadata {

    private String endpointName;

    private List<String> metadata;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointMetadata that = (EndpointMetadata) o;
        return endpointName.equals(that.endpointName) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointName, metadata);
    }

    @Override
    public String toString() {
        return "EndpointMetadata{" +
                "endpointName='" + endpointName + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
