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

import java.util.Collections;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"adabas", Consts.LEARNING_MODE, Consts.CONNECTOR_MODE})
public class AdabasSoftwareAGTests {

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
        Utils.ReceiveMessages("LFPM_TNUOT_HAYOM_schema", "LFPM_TNUOT_HAYOM_schema.json", dataPipelineConfigGenerator);
        DataPipelineConfig dataPipelineConfig = dataPipelineConfigGenerator.getDataPipelineConfig();
        List<DataPipelineConfig.SpaceType> spaceTypeDefinitions = dataPipelineConfig.getSpaceTypes();

        Assert.assertNotNull(spaceTypeDefinitions);

        // user sets space id attribute
        DataPipelineConfig.SpaceType spaceType = spaceTypeDefinitions.get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        // connector mode - creating types on startup
        metadataHandler.createTypesInSpace(spaceTypeDefinitions);

        cdcOperationResolver.reinit(dataPipelineConfig);

        // connector mode - getting data
        Utils.ReceiveMessages("LFPM_TNUOT_HAYOM", "LFPM_TNUOT_HAYOM.json", dataHandler);

        // assertion
        SpaceDocument docById = space.readById(new IdQuery<>("LFPM_TNUOT_HAYOM", 15));
        Assert.assertNotNull(docById);
        Assert.assertEquals(159140l, (long)((docById.getProperty("CHESBON"))));
    }

}
