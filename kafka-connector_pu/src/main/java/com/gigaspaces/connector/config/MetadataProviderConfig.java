package com.gigaspaces.connector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "metadata-provider")
@Profile(Consts.LEARNING_MODE)
public class MetadataProviderConfig {

    private Method method;
    private List<String> topics;
    private List<String> idColumnNames;
    private String parser;
    private TypesMapping typesMapping;
    private SchemalessJsonMetadataParser schemalessJsonMetadataParser;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getIdColumnNames() {
        return idColumnNames;
    }

    public void setIdColumnNames(List<String> idColumnNames) {
        this.idColumnNames = idColumnNames;
    }

    public String getParser() {
        return parser;
    }

    public void setParser(String parser) {
        this.parser = parser;
    }

    public TypesMapping getTypesMapping() {
        return typesMapping;
    }

    public void setTypesMapping(TypesMapping typesMapping) {
        this.typesMapping = typesMapping;
    }

    public static enum Method {
        basedondata,
        IIDR
    }

    public static class TypesMapping {
        private boolean useStringForUnlistedTypes;
        private List<SourceToDih> map;

        public boolean isUseStringForUnlistedTypes() {
            return useStringForUnlistedTypes;
        }

        public void setUseStringForUnlistedTypes(boolean useStringForUnlistedTypes) {
            this.useStringForUnlistedTypes = useStringForUnlistedTypes;
        }

        public List<SourceToDih> getMap() {
            return map;
        }

        public void setMap(List<SourceToDih> map) {
            this.map = map;
        }
    }

    public static class SourceToDih {
        private String sourceType;
        private String dihType;

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getDihType() {
            return dihType;
        }

        public void setDihType(String dihType) {
            this.dihType = dihType;
        }
    }

    public SchemalessJsonMetadataParser getSchemalessJsonMetadataParser() {
        return schemalessJsonMetadataParser;
    }

    public void setSchemalessJsonMetadataParser(SchemalessJsonMetadataParser schemalessJsonMetadataParser) {
        this.schemalessJsonMetadataParser = schemalessJsonMetadataParser;
    }

    public static class SchemalessJsonMetadataParser {
        String namePattern;
        String dataRoot;

        public String getNamePattern() {
            return namePattern;
        }

        public void setNamePattern(String namePattern) {
            this.namePattern = namePattern;
        }

        public String getDataRoot() {
            return dataRoot;
        }

        public void setDataRoot(String dataRoot) {
            this.dataRoot = dataRoot;
        }
    }
}
