package com.gigaspaces.connector.plugins.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.config.SpaceTypeBuilder;
import com.gigaspaces.connector.data.DataException;
import com.gigaspaces.connector.data.TypesMapper;
import com.gigaspaces.connector.kafka.Learning.ConsumerLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebeziumMetadataProvider implements MetadataProvider, MetadataProviderBasedOnData {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerLearning.class);
    private TypesMapper typesMapper;

    private String dataSample;
    private MetadataProviderConfig config;

    @Override
    public void putConfiguration(MetadataProviderConfig config) {
        this.config = config;
        this.typesMapper = new TypesMapper(config.getTypesMapping());
    }

    @Override
    public List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions() {
        ArrayList<DataPipelineConfig.SpaceType> retval = new ArrayList<>();
        HashMap<String, Object> o;
        ObjectMapper mapper = new ObjectMapper();
        try {
            o = mapper.readerFor(Map.class).readValue(dataSample);
        } catch (JsonProcessingException e) {
            String message = MessageFormatter.format("Unable to parse message as json:\n{}", dataSample).getMessage();
            logger.error(message, e);
            throw new DataException(message);
        }

        Map<String, Object> payload = (Map<String, Object>) o.get("payload");
        String topicName = getTopicName(payload);
        List tableChanges = (List) payload.get("tableChanges");
        for (Object tableChangeObj : tableChanges) {
            Map<String, Object> tableChange = (Map<String, Object>) tableChangeObj;
            String type = (String) tableChange.get("type");
            if (!"CREATE".equals(type)) {
                logger.warn("DDL statement of type '{}' is not supported. Skipping.", type);
                continue;
            }
            String tableName = (String) tableChange.get("id");
            tableName = tableName.replace("\"", "");

            Map<String, Object> table = (Map<String, Object>) tableChange.get("table");
            List primaryKeyColumnNames = (List) table.get("primaryKeyColumnNames");
            List columns = (List) table.get("columns");

            DataPipelineConfig.SpaceType spaceType = createType(tableName, columns, primaryKeyColumnNames);
            DataPipelineConfig.DataSource dataSource = new DataPipelineConfig.DataSource();
            dataSource.setTopic(topicName);
            spaceType.setDataSource(dataSource);

            retval.add(spaceType);
        }

        return retval;
    }

    private String getTopicName(Map<String, Object> payload) {
        Map<String, String> source = (Map<String, String>) payload.get("source");
        String name = source.get("name");
        String schema = source.get("schema");
        String table = source.get("table");
        return name + '.' + schema + '.' + table;
    }

    private DataPipelineConfig.SpaceType createType(String tableName, List columns, List primaryKeyColumnNames) {
        SpaceTypeBuilder spaceTypeBuilder = new SpaceTypeBuilder(tableName);

        if (primaryKeyColumnNames.size() != 1)
            throw new DataException("Table '{}' has {} columns in primary key while only one column in primary key is supported now.",
                    tableName, primaryKeyColumnNames.size());

        spaceTypeBuilder.idProperty((String) primaryKeyColumnNames.get(0));

        ArrayList<DataPipelineConfig.Property> properties = new ArrayList<>();
        for (Object columnObj : columns) {
            Map<String, String> column = (Map<String, String>) columnObj;
            String name = column.get("name");
            String typeName = column.get("typeName");

            String spaceDataType = typesMapper.getDihType(typeName);
            DataPipelineConfig.Property property = new DataPipelineConfig.Property();
            property.setName(name);
            property.setType(spaceDataType);
            property.setSelector(name);
            properties.add(property);
        }

        spaceTypeBuilder.setProperties(properties);
        DataPipelineConfig.SpaceType spaceType = spaceTypeBuilder.create();
        return spaceType;
    }

    @Override
    public DataPipelineConfig.Cdc createCdcConfig() {
        DataPipelineConfig.Cdc cdc = new DataPipelineConfig.Cdc();
        DataPipelineConfig.Operations operations = new DataPipelineConfig.Operations();
        cdc.setOperations(operations);

        DataPipelineConfig.InsertOperation insertOperation = new DataPipelineConfig.InsertOperation();
        insertOperation.setIfExists(DataPipelineConfig.IfExists.update);
        insertOperation.setDefaultOperation(true);
        insertOperation.setPropertiesSelectorTemplate("$.payload.after.%s");
        operations.setInsert(insertOperation);

        DataPipelineConfig.UpdateOperation updateOperation = new DataPipelineConfig.UpdateOperation();
        updateOperation.setIfNotExists(DataPipelineConfig.IfNotExists.insert);
        updateOperation.setConditions(new ArrayList<>());
        DataPipelineConfig.Condition updateCondition = new DataPipelineConfig.Condition();
        updateCondition.setSelector("$.payload.op");
        updateCondition.setValue("u");
        updateOperation.getConditions().add(updateCondition);
        updateOperation.setPropertiesSelectorTemplate("$.payload.after.%s");
        operations.setUpdate(updateOperation);

        DataPipelineConfig.Operation deleteOperation = new DataPipelineConfig.Operation();
        deleteOperation.setConditions(new ArrayList<>());
        DataPipelineConfig.Condition deleteCondition = new DataPipelineConfig.Condition();
        deleteCondition.setSelector("$.payload.op");
        deleteCondition.setValue("d");
        deleteOperation.getConditions().add(deleteCondition);
        deleteOperation.setPropertiesSelectorTemplate("$.payload.before.%s");
        operations.setDelete(deleteOperation);

        return cdc;
    }

    @Override
    public String getDataFormat() {
        return "JSON";
    }

    @Override
    public void putDataSample(String topic, Object data) {
        dataSample = (String) data;
    }
}
