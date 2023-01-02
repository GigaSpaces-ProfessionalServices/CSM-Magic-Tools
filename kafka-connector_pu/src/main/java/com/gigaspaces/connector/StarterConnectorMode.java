/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gigaspaces.connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.metadata.MetadataHandler;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import javax.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class StarterConnectorMode {
    private static final Logger logger = LoggerFactory.getLogger(StarterConnectorMode.class);

    @Resource
    private GigaSpace gigaSpace;

    @Resource
    private MetadataHandler metadataHandler;

    @Autowired
    private DataPipelineConfig dataPipelineConfig;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Connector mode started");

        logger.info("Initialized: connected to space {}", gigaSpace.getSpaceName());
        int count = gigaSpace.count(null);
        logger.info("Entries in space: {}", count);

        List<DataPipelineConfig.SpaceType> spaceTypes = dataPipelineConfig.getSpaceTypes();

        metadataHandler.createTypesInSpace(spaceTypes);
    }

    @PostConstruct
    public void initialize() {
        logger.info("Bean initialized");
        logger.info("Initialized: connected to space {}", gigaSpace.getSpaceName());
        // Your code goes here, for example:
        int count = gigaSpace.count(null);
        logger.info("Entries in space: {}", count);
    }
}
