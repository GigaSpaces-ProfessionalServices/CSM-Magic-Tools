package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.helpers.Utils;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.document.DocumentProperties;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.query.IdQuery;
import net.minidev.json.JSONArray;
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
public class SchemalessJsonLearningAndConnectorTests {

    @Autowired
    private GigaSpace space;
    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    @Autowired
    private MetadataHandler metadataHandler;
    @Autowired
    private DataHandler dataHandler;
    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;

    @Test
    public void testBasic() {
        // learning mode
        Utils.ReceiveMessages("person", "person.json", dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();

        // user sets space id attribute
        DataPipelineConfig.SpaceType spaceType = spaceTypeDefinitions.get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        // connector mode
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        Utils.ReceiveMessages("person", "person.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("person", 1));
        Assert.assertNotNull(docById);
        Assert.assertTrue(docById.getProperty("Intlist") instanceof List);
        Assert.assertTrue(docById.getProperty("Objlist") instanceof List);
        Assert.assertEquals(3, ((List) docById.getProperty("Intlist")).size());
        Assert.assertEquals(2, ((List) docById.getProperty("Objlist")).size());
        List objlist = (List) docById.getProperty("Objlist");
        Object o = objlist.get(0);
        Assert.assertTrue(o instanceof DocumentProperties);
    }

    @Test
    public void testBbwPrices() {
        // learning mode
        Utils.ReceiveMessages("prices", "bbw-prices.json", dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();

        // user sets space id attribute
        DataPipelineConfig.SpaceType spaceType = spaceTypeDefinitions.get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        // connector mode
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        Utils.ReceiveMessages("prices", "bbw-prices.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("prices", "025140007"));
        Assert.assertNotNull(docById);
    }

    @Test
    public void testEmptyList() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object emptyList = docById.getProperty("EmptyList");
        Assert.assertTrue(emptyList instanceof List);
        Assert.assertFalse(emptyList instanceof JSONArray);
        Assert.assertEquals(0, ((List) emptyList).size());
    }

    @Test
    public void testListInList() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object listInListObj = docById.getProperty("ListInList");
        Assert.assertTrue(listInListObj instanceof List);
        Assert.assertFalse(listInListObj instanceof JSONArray);
        List list = (List) listInListObj;
        Assert.assertEquals(1, list.get(0));
        Assert.assertFalse(list.get(1) instanceof JSONArray);
    }

    @Test
    public void testListMixedTypes() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object listMixedTypesObj = docById.getProperty("ListMixedTypes");
        Assert.assertTrue(listMixedTypesObj instanceof List);
        Assert.assertFalse(listMixedTypesObj instanceof JSONArray);
        List list = (List) listMixedTypesObj;
        Assert.assertEquals(1, list.get(0));
        Assert.assertEquals("a", list.get(1));
        Assert.assertNull(list.get(2));
    }

    @Test
    public void testListDocsWithVaryingPropsList() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object listDocsWithVaryingPropsListObj = docById.getProperty("ListDocsWithVaryingPropsList");
        Assert.assertTrue(listDocsWithVaryingPropsListObj instanceof List);
        Assert.assertFalse(listDocsWithVaryingPropsListObj instanceof JSONArray);
        List list = (List) listDocsWithVaryingPropsListObj;

        Assert.assertEquals(DocumentProperties.class, list.get(0).getClass());
        DocumentProperties dp1 = (DocumentProperties) list.get(0);
        DocumentProperties dp2 = (DocumentProperties) list.get(1);

        Assert.assertEquals(1, (int) dp1.getProperty("a"));
        Assert.assertEquals(2, (int) dp2.getProperty("a"));

        Assert.assertEquals(true, dp1.getProperty("b"));
        Assert.assertEquals(3, (int) dp2.getProperty("b"));

        Assert.assertNull(dp1.getProperty("c"));
        Assert.assertEquals("text", dp2.getProperty("c"));
    }

    @Test
    public void testListDocsAndSimpleTypes() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object property = docById.getProperty("ListDocsAndSimpleTypes");
        Assert.assertTrue(property instanceof List);
        Assert.assertFalse(property instanceof JSONArray);
        List list = (List) property;

        Assert.assertEquals(1, (int) list.get(0));
        Assert.assertEquals("a", list.get(1));
        Assert.assertTrue(list.get(2) instanceof DocumentProperties);
        DocumentProperties documentProperties = (DocumentProperties) list.get(2);
        Assert.assertEquals("bar", documentProperties.getProperty("foo"));
    }

    @Test
    public void testDocWithArrayProp() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object property = docById.getProperty("DocWithArrayProp");
        Assert.assertTrue(property instanceof DocumentProperties);
        DocumentProperties documentProperties = (DocumentProperties) property;
        Assert.assertEquals("b", documentProperties.getProperty("a"));
        Assert.assertTrue(documentProperties.getProperty("c") instanceof List);
        Assert.assertFalse(documentProperties.getProperty("c") instanceof JSONArray);
        Assert.assertEquals(3, ((List)documentProperties.getProperty("c")).size());
    }

    @Test
    public void testCrazyMix() {
        SpaceDocument docById = learnAndReadAdvancedExample();
        Object property = docById.getProperty("CrazyMix");
        Assert.assertTrue(property instanceof DocumentProperties);
        DocumentProperties documentProperties = (DocumentProperties) property;
        Assert.assertEquals("b", documentProperties.getProperty("a"));
        Assert.assertTrue(documentProperties.getProperty("c") instanceof List);
        Assert.assertFalse(documentProperties.getProperty("c") instanceof JSONArray);
        List c = (List) documentProperties.getProperty("c");
        Assert.assertEquals(1, c.get(0));
        Assert.assertTrue(c.get(1) instanceof DocumentProperties);
        DocumentProperties c_dp = (DocumentProperties) c.get(1);
        Assert.assertEquals("e", c_dp.getProperty("d"));
        Assert.assertTrue(c_dp.getProperty("f") instanceof List);
        Assert.assertFalse(c_dp.getProperty("f") instanceof JSONArray);
        Assert.assertNull(c_dp.getProperty("g"));

        List c_dp_f = (List) c_dp.getProperty("f");
        Assert.assertEquals(2, c_dp_f.size());
    }

    private SpaceDocument learnAndReadAdvancedExample() {
        Utils.ReceiveMessages("advanced", "advanced-nesting.json", dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();

        // user sets space id attribute
        DataPipelineConfig.SpaceType spaceType = spaceTypeDefinitions.get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        // connector mode
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        Utils.ReceiveMessages("advanced", "advanced-nesting.json", dataHandler);

        return space.readById(new IdQuery<>("advanced", 1));
    }

}
