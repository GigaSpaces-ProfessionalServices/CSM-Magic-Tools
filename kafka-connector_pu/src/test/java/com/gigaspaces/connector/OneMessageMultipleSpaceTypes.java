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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"one-message-multiple-space-types", Consts.CONNECTOR_MODE})
public class OneMessageMultipleSpaceTypes {

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private GigaSpace space;

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    @Test
    public void testInsertUpdate() {
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());

        Utils.ReceiveMessages("customers", "hvr-insert-update.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(2, count);

        SpaceDocument docById = space.readById(new IdQuery<>("customers", 1L));
        Assert.assertNotNull(docById);
        Assert.assertEquals("Alice", docById.getProperty("name"));
        Assert.assertEquals("Teacher", docById.getProperty("job"));

        SpaceDocument docById2 = space.readById(new IdQuery<>("customers2", 1L));
        Assert.assertNotNull(docById2);
        Assert.assertEquals("Alice", docById2.getProperty("name2"));
        Assert.assertEquals("Teacher", docById2.getProperty("job2"));
    }
}