package com.gigaspaces.connector;

import com.gigaspaces.connector.cdc.CdcOperationResolver;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.config.MetadataProviderConfig;
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

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"debezium", Consts.LEARNING_MODE, Consts.CONNECTOR_MODE})
public class DebeziumTests {

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private GigaSpace space;

    @Autowired
    private MetadataProviderConfig metadataProviderConfig;

    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;

    @Autowired
    private CdcOperationResolver cdcOperationResolver;

    @Test
    public void testRefresh() {
        // learning mode
        Utils.ReceiveMessages("server1", "debezium-ddl.json", dataPipelineConfigGenerator);
        DataPipelineConfig dataPipelineConfig = dataPipelineConfigGenerator.getDataPipelineConfig();
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfig.getSpaceTypes();

        Assert.assertNotNull(spaceTypeDefinitions);

        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        cdcOperationResolver.reinit(dataPipelineConfig);

        // connector mode - getting data
        Utils.ReceiveMessages("server1.dbo.products", "debezium-refresh.json", dataHandler);

        // assertion
        SpaceDocument docById = space.readById(new IdQuery<>("testDB.dbo.products", 101));
        Assert.assertNotNull(docById);
        Assert.assertEquals("scooter", docById.getProperty("name"));
    }

    @Test
    public void testInsertDelete() {
        // learning mode
        Utils.ReceiveMessages("server1", "debezium-ddl.json", dataPipelineConfigGenerator);
        DataPipelineConfig dataPipelineConfig = dataPipelineConfigGenerator.getDataPipelineConfig();
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfig.getSpaceTypes();

        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        cdcOperationResolver.reinit(dataPipelineConfig);

        // connector mode - getting data
        Utils.ReceiveMessages("server1.dbo.products", "debezium-insert-delete.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testInsertUpdate() {
        // learning mode
        Utils.ReceiveMessages("server1", "debezium-ddl.json", dataPipelineConfigGenerator);
        DataPipelineConfig dataPipelineConfig = dataPipelineConfigGenerator.getDataPipelineConfig();
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfig.getSpaceTypes();

        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        cdcOperationResolver.reinit(dataPipelineConfig);

        // connector mode - getting data
        Utils.ReceiveMessages("server1.dbo.products", "debezium-insert-update.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(1, count);

        // assertion
        SpaceDocument docById = space.readById(new IdQuery<>("testDB.dbo.products", 101));
        Assert.assertNotNull(docById);
        Assert.assertEquals("scooter-update", docById.getProperty("name"));
        Assert.assertEquals("Small 2-wheel scooter", docById.getProperty("description"));
    }

}
