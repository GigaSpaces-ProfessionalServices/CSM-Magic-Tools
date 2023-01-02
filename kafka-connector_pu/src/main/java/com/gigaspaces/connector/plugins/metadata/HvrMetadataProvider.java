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

import java.util.*;

public class HvrMetadataProvider implements MetadataProvider, MetadataProviderBasedOnData {

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
        HashMap<String, Object> o;
        ObjectMapper mapper = new ObjectMapper();
        try {
            o = mapper.readerFor(Map.class).readValue(dataSample);
        } catch (JsonProcessingException e) {
            String message = MessageFormatter.format("Unable to parse message as json:\n{}", dataSample).getMessage();
            logger.error(message, e);
            throw new DataException(message);
        }

        Map<String, Object> schema = (Map<String, Object>) o.get("schema");
        String name = (String) schema.get("name");
        SpaceTypeBuilder spaceTypeBuilder = new SpaceTypeBuilder(name);

        List<Object> fields = (List<Object>) schema.get("fields");
        ArrayList<DataPipelineConfig.Property> properties = new ArrayList<>();
        for (Object f : fields) {
            Map<String, String> fmap = (Map<String, String>) f;
            String field = fmap.get("field");
            if (field.startsWith("hvr_")) continue; // those are hvr metadata fields
            String type = fmap.get("type");
            String dihType = typesMapper.getDihType(type);
            DataPipelineConfig.Property property = new DataPipelineConfig.Property();
            property.setName(field);
            property.setType(dihType);
            property.setSelector("$.payload." + field);
            properties.add(property);
        }

        spaceTypeBuilder.setProperties(properties);
        DataPipelineConfig.SpaceType spaceType = spaceTypeBuilder.create();

        DataPipelineConfig.DataSource dataSource = new DataPipelineConfig.DataSource();
        spaceType.setDataSource(dataSource);

        return Arrays.asList(spaceType);
    }

    @Override
    public DataPipelineConfig.Cdc createCdcConfig() {
        DataPipelineConfig.Cdc cdc = new DataPipelineConfig.Cdc();
        DataPipelineConfig.Operations operations = new DataPipelineConfig.Operations();
        cdc.setOperations(operations);

        DataPipelineConfig.InsertOperation insertOperation = new DataPipelineConfig.InsertOperation();
        insertOperation.setIfExists(DataPipelineConfig.IfExists.update);
        insertOperation.setDefaultOperation(true);
        operations.setInsert(insertOperation);

        DataPipelineConfig.UpdateOperation updateOperation = new DataPipelineConfig.UpdateOperation();
        updateOperation.setIfNotExists(DataPipelineConfig.IfNotExists.insert);
        updateOperation.setConditions(new ArrayList<>());
        DataPipelineConfig.Condition updateCondition = new DataPipelineConfig.Condition();
        updateCondition.setSelector("$.payload.hvr_operation_type");
        updateCondition.setValue(2);
        updateOperation.getConditions().add(updateCondition);
        operations.setUpdate(updateOperation);

        DataPipelineConfig.Operation deleteOperation = new DataPipelineConfig.Operation();
        deleteOperation.setConditions(new ArrayList<>());
        DataPipelineConfig.Condition deleteCondition = new DataPipelineConfig.Condition();
        deleteCondition.setSelector("$.payload.hvr_operation_type");
        deleteCondition.setValue(0);
        deleteOperation.getConditions().add(deleteCondition);
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
