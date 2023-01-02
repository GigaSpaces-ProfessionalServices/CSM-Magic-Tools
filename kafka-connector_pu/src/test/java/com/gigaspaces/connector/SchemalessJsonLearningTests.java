package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.helpers.Utils;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import org.junit.Assert;
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

import java.util.Collections;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"json-learning", Consts.LEARNING_MODE, Consts.CONNECTOR_MODE})
public class SchemalessJsonLearningTests {

    @Autowired
    private GigaSpace space;
    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;
    @Autowired
    private MetadataProviderConfig metadataProviderConfig;

    @Test
    public void testBasic() {
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("person", "person.json");

        List<DataPipelineConfig.Property> properties = spaceTypeDefinitions.get(0).getProperties();
        Assert.assertNotNull(properties);
        Assert.assertEquals(6, properties.size());
        DataPipelineConfig.DataSource dataSource = spaceTypeDefinitions.get(0).getDataSource();
        Assert.assertEquals("person", dataSource.getTopic());

        DataPipelineConfig.Property property = properties.get(0);
        Assert.assertEquals("ID", property.getName());
        Assert.assertEquals("java.lang.Integer", property.getType());
        Assert.assertEquals("$.ID", property.getSelector());
    }

    @Test
    public void testBbwPrices() {
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("prices", "bbw-prices.json");

        List<DataPipelineConfig.Property> properties = spaceTypeDefinitions.get(0).getProperties();
        Assert.assertNotNull(properties);
        Assert.assertEquals(3, properties.size());
        DataPipelineConfig.DataSource dataSource = spaceTypeDefinitions.get(0).getDataSource();
        Assert.assertEquals("prices", dataSource.getTopic());

        DataPipelineConfig.Property property = properties.get(1);
        Assert.assertEquals("dimensions", property.getName());
        Assert.assertNull(property.getType());
        Assert.assertEquals("$.dimensions", property.getSelector());
        Assert.assertNotNull(property.getProperties());

        DataPipelineConfig.Property nestedProp = property.getProperties().get(0);
        Assert.assertEquals("length", nestedProp.getName());
        Assert.assertEquals("java.lang.Double", nestedProp.getType());
        Assert.assertEquals(".length", nestedProp.getSelector());

        DataPipelineConfig.Property nestedListProp = properties.get(2);
        Assert.assertEquals("prices", nestedListProp.getName());
        Assert.assertNotNull(nestedListProp.getAttributes());
        Assert.assertTrue(nestedListProp.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.list));
        Assert.assertNotNull(nestedListProp.getProperties());
    }

    @Test
    public void testBbwProducts() {
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("products", "bbw-products.json");

        List<DataPipelineConfig.Property> properties = spaceTypeDefinitions.get(0).getProperties();
        Assert.assertNotNull(properties);
        Assert.assertEquals(14, properties.size());
        DataPipelineConfig.DataSource dataSource = spaceTypeDefinitions.get(0).getDataSource();
        Assert.assertEquals("products", dataSource.getTopic());

        DataPipelineConfig.Property property = properties.get(13);
        List<DataPipelineConfig.Property> nestedProps = property.getProperties();
        Assert.assertNotNull(nestedProps);
        Assert.assertEquals(9, nestedProps.size());
    }

    @Test
    public void testAssignIdPropertyDuringLearningPhase() {
        metadataProviderConfig.setIdColumnNames(Collections.singletonList("ID"));
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = createSpaceTypeDefinitions("people", "person.json");

        DataPipelineConfig.Property firstProp = spaceTypeDefinitions.get(0).getProperties().get(0);
        Assert.assertEquals("ID", firstProp.getName());
        Assert.assertTrue(firstProp.getAttributes().contains(DataPipelineConfig.Property.PropertyAttribute.spaceid));
    }


    private List<DataPipelineConfig.SpaceType> createSpaceTypeDefinitions(String topic, String filename) {
        Utils.ReceiveMessages(topic, filename, dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();
        return spaceTypeDefinitions;
    }

}
