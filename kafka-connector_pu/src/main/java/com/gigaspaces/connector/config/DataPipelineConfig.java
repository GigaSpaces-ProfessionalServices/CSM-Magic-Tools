package com.gigaspaces.connector.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.metadata.index.SpaceIndexType;

import java.util.List;

public class DataPipelineConfig {

    private String dataFormat;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Cdc cdc;
    private List<SpaceType> spaceTypes;

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public List<SpaceType> getSpaceTypes() {
        return spaceTypes;
    }

    public void setSpaceTypes(List<SpaceType> spaceTypes) {
        this.spaceTypes = spaceTypes;
    }

    public static class SpaceType {

        private String name;
        private DataSource dataSource;
        private List<Property> properties;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Index> indexes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public void setProperties(List<Property> Properties) {
            this.properties = Properties;
        }

        public List<Index> getIndexes() {
            return indexes;
        }

        public void setIndexes(List<Index> indexes) {
            this.indexes = indexes;
        }
    }

    public Cdc getCdc() {
        return cdc;
    }

    public void setCdc(Cdc cdc) {
        this.cdc = cdc;
    }

    public static class Cdc {
        private Operations operations;

        public Operations getOperations() {
            return operations;
        }

        public void setOperations(Operations operations) {
            this.operations = operations;
        }

    }

    public static class Condition {
        private String selector;
        private Object value;

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static class DataSource {

        private String topic;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<com.gigaspaces.connector.config.DataPipelineConfig.Condition> conditions;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public List<com.gigaspaces.connector.config.DataPipelineConfig.Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<com.gigaspaces.connector.config.DataPipelineConfig.Condition> conditions) {
            this.conditions = conditions;
        }
    }

    public static class Property {

        private String name;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String type;
        private String selector;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String defaultValue;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<PropertyAttribute> attributes;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Property> properties;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public void setProperties(List<Property> Properties) {
            this.properties = Properties;
        }

        public List<PropertyAttribute> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<PropertyAttribute> attributes) {
            this.attributes = attributes;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public static enum PropertyAttribute {
            spaceid,
            routingkey,
            list
        }
    }

    public static class Index {
        private List<String> properties;
        private SpaceIndexType type;
        private boolean unique;

        public List<String> getProperties() {
            return properties;
        }

        public void setProperties(List<String> Properties) {
            this.properties = Properties;
        }

        public SpaceIndexType getType() {
            return type;
        }

        public void setType(SpaceIndexType type) {
            this.type = type;
        }

        public boolean getUnique() {
            return unique;
        }

        public void setUnique(boolean unique) {
            this.unique = unique;
        }
    }

    public static class Operations {
        private InsertOperation insert;
        private UpdateOperation update;
        private Operation delete;

        public InsertOperation getInsert() {
            return insert;
        }

        public void setInsert(InsertOperation insert) {
            this.insert = insert;
        }

        public UpdateOperation getUpdate() {
            return update;
        }

        public void setUpdate(UpdateOperation update) {
            this.update = update;
        }

        public Operation getDelete() {
            return delete;
        }

        public void setDelete(Operation delete) {
            this.delete = delete;
        }
    }

    public static class Operation {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Condition> conditions;
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        private boolean defaultOperation;
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        private String propertiesSelectorTemplate;

        public List<Condition> getConditions() {
            return conditions;
        }

        public void setConditions(List<Condition> conditions) {
            this.conditions = conditions;
        }

        public boolean isDefaultOperation() {
            return defaultOperation;
        }

        public void setDefaultOperation(boolean defaultOperation) {
            this.defaultOperation = defaultOperation;
        }

        public String getPropertiesSelectorTemplate() {
            return propertiesSelectorTemplate;
        }

        public void setPropertiesSelectorTemplate(String propertiesSelectorTemplate) {
            this.propertiesSelectorTemplate = propertiesSelectorTemplate;
        }
    }

    public static class InsertOperation extends Operation {
        private IfExists ifExists = IfExists.override;

        public IfExists getIfExists() {
            return ifExists;
        }

        public void setIfExists(IfExists ifExists) {
            this.ifExists = ifExists;
        }
    }

    public enum IfExists {
        override,
        update,
        skip,
        terminate,
    }

    public static class UpdateOperation extends Operation {
        private IfNotExists ifNotExists = IfNotExists.insert;

        public IfNotExists getIfNotExists() {
            return ifNotExists;
        }

        public void setIfNotExists(IfNotExists ifNotExists) {
            this.ifNotExists = ifNotExists;
        }
    }

    public enum IfNotExists {
        insert,
        skip,
        terminate
    }

}
