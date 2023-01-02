package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.helpers.Utils;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.query.IdQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles({"lists", Consts.CONNECTOR_MODE})

public class ListsTests {

    @Autowired
    private GigaSpace space;

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    @Before
    public void before() {
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());
    }

    @Test
    public void testListProperty() {
        Utils.ReceiveMessages("Person", "person.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("Person", 1L));
        Assert.assertNotNull(docById);
        Assert.assertTrue(docById.getProperty("Intlist") instanceof List);
        Assert.assertTrue(docById.getProperty("Objlist") instanceof List);
        List intlist = (List) docById.getProperty("Intlist");
        Assert.assertEquals(3, intlist.size());
        Assert.assertEquals(1, intlist.get(0));
        Assert.assertEquals(2, intlist.get(1));
        Assert.assertEquals(3, intlist.get(2));
        List objlist = (List) docById.getProperty("Objlist");
        Assert.assertEquals(2, objlist.size());
    }

}
