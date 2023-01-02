package com.gigaspaces.connector;

import com.gigaspaces.connector.config.ConfigurationException;
import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.helpers.ApplicationTestBeans;
import com.gigaspaces.connector.helpers.TestSpaceConfig;
import com.gigaspaces.connector.metadata.MetadataHandler;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
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

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestSpaceConfig.class, ApplicationTestBeans.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles({"metadata", Consts.CONNECTOR_MODE})
public class MetadataHandlerTests {

    @Autowired
    private MetadataHandler metadataHandler;

    @Autowired
    private GigaSpace space;

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    @Test
    public void testHappyPath() {
        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());

        SpaceTypeDescriptor person = space.getTypeManager().getTypeDescriptor("Person");
        Assert.assertNotNull(person);
    }

    @Test(expected = ConfigurationException.class)
    public void testConfigExceptionWhenSpaceIdNotSet() {
        DataPipelineConfig.SpaceType spaceType = dataPipelineConfig.getSpaceTypes().get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(0);
        property.setAttributes(null);

        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());
    }

    @Test(expected = ConfigurationException.class)
    public void testConfigExceptionWhenMultipleSpaceId() {
        DataPipelineConfig.SpaceType spaceType = dataPipelineConfig.getSpaceTypes().get(0);
        DataPipelineConfig.Property property = spaceType.getProperties().get(1);
        property.setAttributes(Collections.singletonList(DataPipelineConfig.Property.PropertyAttribute.spaceid));

        metadataHandler.createTypesInSpace(dataPipelineConfig.getSpaceTypes());
    }
}
