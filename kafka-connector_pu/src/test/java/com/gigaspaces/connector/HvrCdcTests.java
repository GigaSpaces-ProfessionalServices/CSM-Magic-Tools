package com.gigaspaces.connector;

import com.gigaspaces.connector.cdc.CdcOperationResolver;
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
@ActiveProfiles({"hvr-cdc", Consts.CONNECTOR_MODE})
public class HvrCdcTests {

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private GigaSpace space;

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    @Autowired
    private CdcOperationResolver cdcOperationResolver;

    @Test
    public void testInsertDelete() {
        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());

        // connector mode - getting data
        Utils.ReceiveMessages("customers", "hvr-insert-delete.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testInsertUpdate() {
        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());

        // connector mode - getting data
        Utils.ReceiveMessages("customers", "hvr-insert-update.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(1, count);

        SpaceDocument docById = space.readById(new IdQuery<>("customers", 1L));
        Assert.assertNotNull(docById);
        Assert.assertEquals("Alice", docById.getProperty("name"));
        Assert.assertEquals("Teacher", docById.getProperty("job"));
    }

    @Test
    public void testInsertDeleteWithoutCdcOperationDefined() {
        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());

        dataPipelineConfig.getCdc().getOperations().setDelete(null);
        cdcOperationResolver.reinit(dataPipelineConfig);

        // connector mode - getting data
        Utils.ReceiveMessages("customers", "hvr-insert-delete.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(1, count);
    }
}
