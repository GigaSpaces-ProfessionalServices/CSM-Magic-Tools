package com.gigaspaces.tierdirectcall.controller;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.tierdirectcall.common.CommonUtil;
import com.gigaspaces.tierdirectcall.dto.UpdateTypeDetail;
import com.gigaspaces.tierdirectcall.service.ConnectionManager;
import com.gigaspaces.tierdirectcall.service.TypeUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/policy")
public class ApiController {
    @Value("${xap.workLocation}")
    String workLocation;
    /* @Value("${space.name}")
     String spaceName;*/
    @Value("${tier.criteria.file}")
    String tierCriteriaFilename;
    private Logger logger = LoggerFactory.getLogger(ApiController.class);

    @PostMapping(value = "/update", consumes = {MediaType.APPLICATION_JSON_VALUE})
    String updateType(@RequestBody UpdateTypeDetail updateTypeDetail) {
        try {
            Map<String, TieredStorageTableConfig> tableConfigMap = CommonUtil.setTableConfigCriteria(tierCriteriaFilename);
            logger.info("tierCriteriaFilename: " + tierCriteriaFilename + ", spaceName: " + updateTypeDetail.getSpaceName() + ", tableConfigMap : " + tableConfigMap);
            logger.info("workLocation: " + workLocation + "/tiered-storage/" + updateTypeDetail.getSpaceName() + "/" + ", fileNameWithPattern: sqlite_db_" + updateTypeDetail.getPartialMemberName() + "*" + (updateTypeDetail.getIsBackup().equals("true") ? "_" : "") + updateTypeDetail.getSpaceName());
            List<String> fileNames = CommonUtil.getFileNamesWithPattern(workLocation + "/tiered-storage/" + updateTypeDetail.getSpaceName() + "/", "sqlite_db_" + updateTypeDetail.getPartialMemberName() + "*" + (updateTypeDetail.getIsBackup().equals("true") ? "_*" : "") + updateTypeDetail.getSpaceName());

            for (String typeName : tableConfigMap.keySet()) {
                TieredStorageTableConfig tableConfig = tableConfigMap.get(typeName);
                logger.info("fileNames : " + fileNames);
                for (String fullMemberName : fileNames) {
                    System.out.println("fullMemberName : " + fullMemberName);
                    System.out.println("tableConfig : " + tableConfig);
                    logger.info("Updating type " + tableConfig);
                    Path work = Paths.get(workLocation).toAbsolutePath();
                    ConnectionManager connectionManager = new ConnectionManager(work, updateTypeDetail.getSpaceName(), fullMemberName);
                    Connection connection = connectionManager.connectToDB();
                    TypeUpdater typeUpdater = new TypeUpdater(connection, typeName, tableConfig);
                    logger.info("before : typeName" + typeName + ", fullMemberName : " + fullMemberName);
                    boolean isUpdated = typeUpdater.update();
                    logger.info("after : typeName" + typeName + ", fullMemberName : " + fullMemberName + ", isUpdated : " + isUpdated);
                    connectionManager.shutDown();
                }
                logger.info("Done updating type " + tableConfig);
            }
            return "SUCCESS";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "ERROR";
        }
    }
}
