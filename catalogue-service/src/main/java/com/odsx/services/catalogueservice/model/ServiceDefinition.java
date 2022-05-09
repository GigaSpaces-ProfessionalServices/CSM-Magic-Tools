package com.odsx.services.catalogueservice.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ServiceDefinition{
    private String name;
    private List<InstanceDefinition> instances;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InstanceDefinition> getInstances() {
        return instances;
    }

    public void setInstances(List<InstanceDefinition> instances) {
        this.instances = instances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDefinition that = (ServiceDefinition) o;
        return name.equals(that.name) && Objects.equals(instances, that.instances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, instances);
    }

    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "name='" + name + '\'' +
                ", instances=" + instances +
                '}';
    }
}
