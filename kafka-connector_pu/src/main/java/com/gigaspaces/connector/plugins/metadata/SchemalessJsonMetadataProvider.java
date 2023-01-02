package com.gigaspaces.connector.plugins.metadata;

import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.config.SpaceTypeBuilder;
import com.gigaspaces.connector.data.TypesMapper;
import com.gigaspaces.connector.kafka.Learning.ConsumerLearning;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemalessJsonMetadataProvider implements MetadataProvider, MetadataProviderBasedOnData {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerLearning.class);
    private TypesMapper typesMapper;

    private String dataSample;
    private String topic;
    private MetadataProviderConfig config;

    @Override
    public void putConfiguration(MetadataProviderConfig config) {
        this.config = config;
        this.typesMapper = new TypesMapper(config.getTypesMapping());
    }

    @Override
    public List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions() {
        DocumentContext parsedJson = JsonPath.parse(dataSample);

        MetadataProviderConfig.SchemalessJsonMetadataParser schemalessJsonMetadataParser = config.getSchemalessJsonMetadataParser();
        String namePattern = schemalessJsonMetadataParser.getNamePattern();

        namePattern = namePattern.replace("{topic-name}", topic);
        namePattern = resolveJsonValuePlaceholders(parsedJson, namePattern);

        // TODO: 30/11/2021 if namepattern does not use any placeholder throw an exception

        SpaceTypeBuilder spaceTypeBuilder = new SpaceTypeBuilder(namePattern);
        String dataRoot = schemalessJsonMetadataParser.getDataRoot();
        Map<String, Object> propsRoot = (Map<String, Object>) parsedJson.read(dataRoot);
        List<DataPipelineConfig.Property> properties = createProperties(propsRoot, dataRoot);
        spaceTypeBuilder.setProperties(properties);
        DataPipelineConfig.SpaceType spaceType = spaceTypeBuilder.create();
        DataPipelineConfig.DataSource dataSource = new DataPipelineConfig.DataSource();
        spaceType.setDataSource(dataSource);
        dataSource.setTopic(topic);

        return Arrays.asList(spaceType);
    }

    @Override
    public DataPipelineConfig.Cdc createCdcConfig() {
        return null;
    }

    @Override
    public String getDataFormat() {
        return "JSON";
    }

    @Override
    public void putDataSample(String topic, Object data) {
        this.topic = topic;
        dataSample = (String) data;
    }

    private List<DataPipelineConfig.Property> createProperties(Map<String, Object> propsRoot, String jsonPath) {
        ArrayList<DataPipelineConfig.Property> retval = new ArrayList<>();
        for (Map.Entry<String, Object> entry : propsRoot.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            DataPipelineConfig.Property property = new DataPipelineConfig.Property();
            property.setName(name);
            String currentPath = jsonPath + '.' + name;

            if (value instanceof Map) {
                List<DataPipelineConfig.Property> properties = createProperties((Map<String, Object>) value, "");
                property.setProperties(properties);
                property.setSelector(currentPath);
            } else if (value instanceof JSONArray) {
                JSONArray arrValue = (JSONArray) value;
                buildListProperty(property, arrValue, currentPath + "[*]");
            } else {
                String propType = value.getClass().getName();
                property.setType(propType);
                property.setSelector(currentPath);
            }

            retval.add(property);
        }

        return retval;
    }

    private void buildListProperty(DataPipelineConfig.Property property, JSONArray arrValue, String currentPath) {
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.list));

        boolean containsDocuments = arrValue.stream().anyMatch(x -> x instanceof Map);
        boolean containsSimpleValues = arrValue.stream().anyMatch(x -> !(x instanceof Map));

        property.setSelector(currentPath);

        if (containsDocuments && containsSimpleValues) {
            // Object can hold both documents and simple values
            property.setType(Object.class.getName());
        } else if (containsDocuments) {
            List<DataPipelineConfig.Property> nestedProps = buildUnionOfDocumentProperties(arrValue, "");
            property.setProperties(nestedProps);
        } else if (containsSimpleValues) {
            String type = getCommonTypeOfArrayValues(arrValue);
            property.setType(type);
        } else {
            // empty array
            property.setType(Object.class.getName());
        }
    }

    private String getCommonTypeOfArrayValues(JSONArray arrValue) {
        Class<?> commonType = arrValue.get(0).getClass();
        boolean allSameType = arrValue.stream().allMatch(x -> x.getClass().equals(commonType));
        return allSameType ? commonType.getName() : Object.class.getName();
    }

    private List<DataPipelineConfig.Property> buildUnionOfDocumentProperties(JSONArray arrValue, String path) {
        LinkedHashMap<String, DataPipelineConfig.Property> union = new LinkedHashMap<>();

        for (Object document : arrValue) {
            Map<String, Object> map = (Map<String, Object>) document;
            List<DataPipelineConfig.Property> properties = createProperties(map, path);
            for (DataPipelineConfig.Property p : properties) {
                if (union.containsKey(p.getName())) {
                    DataPipelineConfig.Property unionProp = union.get(p.getName());
                    if (!unionProp.getType().equals(p.getType())) {
                        // Object type is always the lowest common denominator
                        unionProp.setType(Object.class.getName());
                    }
                } else {
                    union.put(p.getName(), p);
                }
            }
        }
        return new ArrayList<>(union.values());
    }

    private String resolveJsonValuePlaceholders(DocumentContext parsedJson, String namePattern) {
        // TODO: 30/11/2021 json path points to non existing location
        String originalString = "";
        String regex = "(.*)(\\{json-value:)(.*)(})(.*)";
        Pattern pattern = Pattern.compile(regex);

        while (!originalString.equals(namePattern)) {
            originalString = namePattern;
            Matcher matcher = pattern.matcher(namePattern);
            if (matcher.matches()) {
                String path = matcher.group(3);

                // TODO: 30/11/2021 what if casting to String fails
                String value = parsedJson.read(path);
                String wholePlaceholder = matcher.group(2) + matcher.group(3) + matcher.group(4);
                namePattern = namePattern.replace(wholePlaceholder, value);
            }
        }

        return namePattern;
    }
}
