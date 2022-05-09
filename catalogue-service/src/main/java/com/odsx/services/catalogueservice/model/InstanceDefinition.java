package com.odsx.services.catalogueservice.model;

import java.util.Objects;

public class InstanceDefinition {

    private String serviceName;
    private String hostName;
    private String portNumber;
    public String getHostName() {
    return hostName;
    }

        public void setHostName(String hostName) {
        this.hostName = hostName;
    }

        public String getPortNumber() {
        return portNumber;
    }

        public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }


        public String getServiceName() {
        return serviceName;
    }

        public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

        @Override
        public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceDefinition that = (InstanceDefinition) o;
        return serviceName.equals(that.serviceName)
                && hostName.equals(that.hostName)
                && portNumber.equals(that.portNumber);
    }

        @Override
        public int hashCode() {
        return Objects.hash(serviceName, hostName, portNumber);
    }

        @Override
        public String toString() {
        return "InstanceDefinition{" +
                "serviceName='" + serviceName + '\'' +
                "hostName='" + hostName + '\'' +
                ", portNumber='" + portNumber + '\'' +
                '}';
    }
}
