package com.gigaspaces.tierdirectcall.controller;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.tierdirectcall.common.CommonUtil;
import com.gigaspaces.tierdirectcall.service.ConnectionManager;
import com.gigaspaces.tierdirectcall.service.TypeUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;

@RestController
@RequestMapping("/policy")
public class ApiController {
    @Value("xap.workLocation")
    String workLocation;
    @Value("space.name")
    String spaceName;
    @Value("tier.criteria.file")
    String tierCriteriaFilename;
    private Logger logger = LoggerFactory.getLogger(ApiController.class);

    //workLocation=app.space.gsOptionExt="-Dcom.gs.work=/
    //spaceName = twinkal will have
    //fullMemberName= twinkal will call rest and pass in api
    // typeName, critria = go over Criteria Tab file
    @PostMapping("/update")
    String updateType(String typeName, String fullMemberName, String spaceName) {
        try {
            List<TieredStorageTableConfig> tableConfigList = CommonUtil.setTableConfigCriteria(tierCriteriaFilename);
            for (TieredStorageTableConfig tableConfig : tableConfigList) {
                logger.info("Updating type " + tableConfig);
                Path work = Paths.get(workLocation).toAbsolutePath();
                ConnectionManager connectionManager = new ConnectionManager(work, spaceName, fullMemberName);
                Connection connection = connectionManager.connectToDB();
                TypeUpdater typeUpdater = new TypeUpdater(connection, typeName, tableConfig);
                typeUpdater.update();
                connectionManager.shutDown();
                logger.info("Done updating type " + tableConfig);
            }
            return "SUCCESS";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "ERROR";
        }
    }
}
