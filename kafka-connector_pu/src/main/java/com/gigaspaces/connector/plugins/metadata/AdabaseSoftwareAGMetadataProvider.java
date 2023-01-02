package com.gigaspaces.connector.plugins.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.config.SpaceTypeBuilder;
import com.gigaspaces.connector.data.DataException;
import com.gigaspaces.connector.data.TypesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdabaseSoftwareAGMetadataProvider implements MetadataProvider, MetadataProviderBasedOnData {

    private static final Logger logger = LoggerFactory.getLogger(AdabaseSoftwareAGMetadataProvider.class);
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

        Map<String, Object> payload = (Map<String, Object>) ((Map<String, Object>) o.get("payload")).get("artdocument");
        String topicName = (String)o.get("type");
        List fields = (List) ((Map<String, Object>) payload.get("fields")).get("field");
        List Columns = new ArrayList();
        ArrayList<DataPipelineConfig.Property> properties = new ArrayList<>();
        for (Object field : fields) {
            Map<String, Object> fieldMetadata = (Map<String, Object>) field;
            String type = (String) fieldMetadata.get("type");
            String name = (String) fieldMetadata.get("name");
            int pre = (Integer)fieldMetadata.get("pre");
            int length = (Integer)fieldMetadata.get("length");
            String dihType = typesMapper.getDihType(type);
            if(type.equalsIgnoreCase("decimal") && dihType.equalsIgnoreCase("string")){
                if(pre > 3 || length > 18){
                    dihType = "java.math.BigDecimal";
                } else if (pre >0 ){
                    dihType = "java.lang.Double";

                } else if (length > 9 ){
                    dihType = "java.lang.Long";
                } else {
                    dihType = "java.lang.Integer";
                }
            }
            else if (dihType.equalsIgnoreCase("string"))
            {
                dihType = "java.lang.String";
            }
            DataPipelineConfig.Property p = new DataPipelineConfig.Property();
            p.setName(name);
            p.setType(dihType);
            p.setSelector("$.payload.artdocument.fields.field[?(@.name=='" + name + "')].value");
            properties.add(p);
        }
        //TODO: do we have an option to define how to get the ID/Routing from Adabas?
        SpaceTypeBuilder spaceTypeBuilder = new SpaceTypeBuilder(topicName);
        spaceTypeBuilder.setProperties(properties);
        DataPipelineConfig.SpaceType spaceType = spaceTypeBuilder.create();


        DataPipelineConfig.DataSource dataSource = new DataPipelineConfig.DataSource();
        dataSource.setTopic(topicName);
        spaceType.setDataSource(dataSource);
        retval.add(spaceType);
        return retval;
    }

    @Override
    public DataPipelineConfig.Cdc createCdcConfig() {
//        DataPipelineConfig.Cdc cdc = new DataPipelineConfig.Cdc();

        return null;
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
