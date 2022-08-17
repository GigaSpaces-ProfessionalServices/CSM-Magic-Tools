package com.gigaspaces.retentionmanager.model;

import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name="object_retention_policy")
public class ObjectRetentionPolicy {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    //@Column(name="obj_type",unique = true)
    @Column(name="obj_type")
    private String objectType;

    @Column(name="retention_period")
    private String retentionPeriod;

    @Column(name="is_active")
    private Boolean active;

    @Column(name="constraintField")
    private String constraintField;
    public ObjectRetentionPolicy(){
        super();
    }

    public ObjectRetentionPolicy(String objectType, String retentionPeriod, String constraintField, Boolean active) {
        this.objectType = objectType;
        this.retentionPeriod = retentionPeriod;
        this.constraintField = constraintField;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public String getConstraintField() {
        return constraintField;
    }

    public void setConstraintField(String constraintField) {
        this.constraintField = constraintField;
    }
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectRetentionPolicy that = (ObjectRetentionPolicy) o;
        return id.equals(that.id) && objectType.equals(that.objectType) &&
                Objects.equals(retentionPeriod, that.retentionPeriod) &&
                Objects.equals(constraintField, that.constraintField) &&
                Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, objectType, retentionPeriod, constraintField, active);
    }

    @Override
    public String toString() {
        return "ObjectRetentionPolicy{" +
                "id=" + id +
                ", objectType='" + objectType + '\'' +
                ", retentionPeriod='" + retentionPeriod + '\'' +
                ", constraintField='" + constraintField + '\'' +
                ", isActive=" + active +
                '}';
    }
}
