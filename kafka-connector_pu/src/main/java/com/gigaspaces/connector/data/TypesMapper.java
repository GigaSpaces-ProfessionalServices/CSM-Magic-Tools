package com.gigaspaces.connector.data;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.MetadataProviderConfig;

import java.util.HashMap;
import java.util.Map;

public class TypesMapper {

    private final Map<String, String> sourceTypeToDihType = new HashMap<>();
    private MetadataProviderConfig.TypesMapping typesMapping;

    public TypesMapper(MetadataProviderConfig.TypesMapping typesMapping) {
        this.typesMapping = typesMapping;
        if (typesMapping.getMap() != null)
            for (MetadataProviderConfig.SourceToDih std : typesMapping.getMap())
                sourceTypeToDihType.put(std.getSourceType(), std.getDihType());
    }

    public String getDihType(String sourceType) {
        String dihType = sourceTypeToDihType.get(sourceType);
        if (dihType == null) {
            if (typesMapping.isUseStringForUnlistedTypes()) {
                return "String";
            } else {
                throw new ConfigurationException(
                        "Check types mapping configuration. Source type '{}' is not defined.", sourceType);
            }
        } else {
            return dihType;
        }
    }

}
