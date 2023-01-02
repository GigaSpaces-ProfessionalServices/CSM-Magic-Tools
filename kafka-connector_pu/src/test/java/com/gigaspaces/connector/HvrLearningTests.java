package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.helpers.Utils;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.query.IdQuery;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles({"hvr-learning", Consts.LEARNING_MODE, Consts.CONNECTOR_MODE})
public class HvrLearningTests {

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private GigaSpace space;

    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;

    @Test
    public void testLearningAndConnector() {
        // learning mode
        Utils.ReceiveMessages("customers", "hvr-message-example.json", dataPipelineConfigGenerator);
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions =
                dataPipelineConfigGenerator.getDataPipelineConfig().getSpaceTypes();

        Assert.assertNotNull(spaceTypeDefinitions);

        // user updates type definitions
        DataPipelineConfig.SpaceType spaceType = spaceTypeDefinitions.get(0);
        spaceType.setName("HVR.Customer"); // user overrides the generated type name

        // user marks ID field
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        // connector mode - getting data
        Utils.ReceiveMessages("customers", "hvr-message-example.json", dataHandler);

        // assertion
        SpaceDocument docById = space.readById(new IdQuery<>("HVR.Customer", 1L));
        Assert.assertNotNull(docById);
        Assert.assertEquals("Alice", docById.getProperty("name"));
    }

}
