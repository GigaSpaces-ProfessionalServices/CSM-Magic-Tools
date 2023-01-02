package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.helpers.Utils;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import com.gigaspaces.connector.metadata.MetadataHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"json-learning", Consts.LEARNING_MODE, Consts.CONNECTOR_MODE})
public class SchemalessJsonLearningNameTests {

    @Autowired
    private MetadataHandler metadataHandler;
    @Autowired
    private DataHandler dataHandler;
    @Autowired
    private GigaSpace space;
    @Autowired
    private DataPipelineConfig dataPipelineConfig;
    @Autowired
    private MetadataProviderConfig metadataProviderConfig;
    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    private MetadataProviderConfig.SchemalessJsonMetadataParser jsonParser;

    @Before
    public void before() {
        jsonParser = new MetadataProviderConfig.SchemalessJsonMetadataParser();
        jsonParser.setDataRoot("$");
        metadataProviderConfig.setSchemalessJsonMetadataParser(jsonParser);
    }

    @Test
    public void testNameBasedOnTopic() {
        jsonParser.setNamePattern("{topic-name}");

        String topic = "person";
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions(topic, "person.json");

        String name = spaceTypeDefinitions.get(0).getName();
        Assert.assertEquals(topic, name);
    }

    @Test
    public void testNameBasedOnTopicWithPattern() {
        jsonParser.setNamePattern("someprefix.{topic-name}");

        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("person", "person.json");

        String name = spaceTypeDefinitions.get(0).getName();
        Assert.assertEquals("someprefix.person", name);
    }

    // TODO: 13/12/2021 make these tests work (broke after adding nested properties)
    @Test
    @Ignore
    public void testNameBasedOnJsonValueWithPattern() {
        jsonParser.setNamePattern("prefix.{json-value:$.payload.source.table}.suffix");

        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("products", "debezium-ddl.json");

        String name = spaceTypeDefinitions.get(0).getName();
        Assert.assertEquals("prefix.products.suffix", name);
    }

    @Test
    @Ignore
    public void testNameBasedOnMultipleJsonValues() {
//        jsonParser.setDataRoot("$.payload.tableChanges[0].table.columns[*].name");
        jsonParser.setNamePattern("Pipeline1.{json-value:$.payload.source.db}.{json-value:$.payload.source.schema}.{json-value:$.payload.source.table}");

        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("products", "debezium-ddl.json");

        String name = spaceTypeDefinitions.get(0).getName();
        Assert.assertEquals("Pipeline1.testDB.dbo.products", name);
    }

    private List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions(String topic, String filename) {
        // create the bean with updated configuration
        DataPipelineConfigGenerator dataPipelineConfigGenerator = new DataPipelineConfigGenerator();
        beanFactory.autowireBean(dataPipelineConfigGenerator);
        dataPipelineConfigGenerator.initialize();

        Utils.ReceiveMessages(topic, filename, dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();
        return spaceTypeDefinitions;
    }

}
