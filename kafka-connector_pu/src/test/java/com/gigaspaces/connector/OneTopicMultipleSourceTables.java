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
@ActiveProfiles({"one-topic-multiple-tables", Consts.CONNECTOR_MODE})
public class OneTopicMultipleSourceTables {

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

        Utils.ReceiveMessages("mixed-topic", "hvr-parent-child.json", dataHandler);

        int count = space.count(null);
        Assert.assertEquals(2, count);

        SpaceDocument customer = space.readById(new IdQuery<>("customers", 1L));
        Assert.assertNotNull(customer);
        Assert.assertEquals("Alice", customer.getProperty("name"));

        SpaceDocument order = space.readById(new IdQuery<>("orders", 100L));
        Assert.assertNotNull(order);
        Assert.assertEquals("Alice order", order.getProperty("description"));
        Assert.assertEquals(1L, (long) order.getProperty("customer_id"));
    }
}
