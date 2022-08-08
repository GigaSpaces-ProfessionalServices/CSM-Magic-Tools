package com.odsx.services.catalogueservice.response;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class EndpointResponse {

    private String endpointName;

    private Integer numberOfInstances;

    private List<String> metadata;

    private String project;

    private String description;
    private String errorMsg;

    private String portNumbers;

    private HealthStatus healthStatus;

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

    public String getPortNumbers() {
        return portNumbers;
    }

    public void setPortNumbers(String portNumbers) {
        this.portNumbers = portNumbers;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    @Override
    public String toString() {
        return "EndpointResponse{" +
                "endpointName='" + endpointName + '\'' +
                ", numberOfInstances=" + numberOfInstances +
                ", metadata=" + metadata +
                ", project='" + project + '\'' +
                ", description='" + description + '\'' +
                ", errorMsg='" + errorMsg + '\'' +
                ", portNumbers='" + portNumbers + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointResponse that = (EndpointResponse) o;
        return endpointName.equals(that.endpointName) && Objects.equals(numberOfInstances, that.numberOfInstances) && Objects.equals(metadata, that.metadata) && Objects.equals(project, that.project) && Objects.equals(description, that.description) && Objects.equals(errorMsg, that.errorMsg) && Objects.equals(portNumbers, that.portNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointName, numberOfInstances, metadata, project, description, errorMsg, portNumbers);
    }
}

