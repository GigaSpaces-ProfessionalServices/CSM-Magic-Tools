package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
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

import static com.gigaspaces.connector.helpers.Utils.ReceiveMessages;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"basic", Consts.CONNECTOR_MODE})
public class BasicTests {

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
    public void testBasic() {
        ReceiveMessages("Person", "person.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("Person", 1L));
        Assert.assertNotNull(docById);
    }

    @Test
    public void testMissingField() {
        ReceiveMessages("Person", "person-missing-field.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("Person", 1L));
        Assert.assertNotNull(docById);
        Assert.assertNull(docById.getProperty("Birthdate"));
    }

    @Test
    public void testDefaultValue() {
        ReceiveMessages("Person", "person.json", dataHandler);

        SpaceDocument docById = space.readById(new IdQuery<>("Person", 1L));
        Assert.assertEquals("TheDefaultValue", docById.getProperty("PropWithDefaultValue"));
    }
}
