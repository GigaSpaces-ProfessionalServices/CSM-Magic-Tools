package com.gigaspaces.objectManagement.controller;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.objectManagement.model.RecordOutcome;
import com.gigaspaces.objectManagement.model.ReportData;
import com.gigaspaces.objectManagement.model.TableOutcome;
import com.gigaspaces.objectManagement.service.DdlParser;
import com.gigaspaces.objectManagement.service.ObjectService;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.gigaspaces.objectManagement.utils.ReportWriter;
import com.gigaspaces.query.IdQuery;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.gigaspaces.objectManagement.utils.DataGeneratorUtils.getPropertyValue;
import static java.nio.file.Files.readAllBytes;

@RestController
public class ObjectController {

    private Logger logger = LoggerFactory.getLogger(ObjectController.class);

    @Autowired
    private GigaSpace gigaSpace;

    @Autowired
    private ObjectService objectService;

    @Autowired
    private DdlParser parser;
    private static final String TYPE_DISTINGUISHER_SUFFIX = "_C";
    private static final int NUMBER_OF_RECORDS = 10;
    Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap = new TreeMap<>();
    Map<String, SpaceTypeDescriptor> suffixedTypeDescriptorMap = new TreeMap<>();
    //ReportData reportData;
    ReportData reportData = new ReportData();

    @Value("${ddl.properties.file.path}")
    private String ddlAndPropertiesBasePath;

    @Value("${space.name}")
    private String spaceName;

    @Value("${table.batch.file.path}")
    private String tableListFilePath;

    @Value("${tier.criteria.file}")
    private String strTierCriteriaFile;

    @Value("${lookup.group}")
    private String lookupGroup;

    @Value("${lookup.locator}")
    private String lookupLocator;

    @GetMapping("/list")
    public JsonArray getObjectList(){
        logger.info("Entering into -> getObjectList");
        try {
            JsonArray jsonArray = objectService.listObjects();
            logger.info("Exiting from -> getObjectList");
            return jsonArray;
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in getObjectList -> "+e.getLocalizedMessage(),e);
            return null;
        }

    }

    @PostMapping("/registertype/batch")
    public JsonObject registerTypeBatch() {
        logger.info("start -- registertype  batch");

        try{
            JsonObject jsonObject= objectService.registerObjectBatch();
            return jsonObject;
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in registerTypeBatch -> "+e.getLocalizedMessage(),e);
            return new JsonObject();
        }


    }

    @PostMapping("/unregistertype")
    public String unregisterType(@RequestParam("type") String type) {
        logger.info("start -- unregistertype");
        logger.info("params received :type=" + type + ",spaceName=" + spaceName);
        try {
            objectService.unregisterObject(type);
            return "success";
        }catch (Exception e){
            e.printStackTrace();
            logger.error("Error in unregisterType -> "+e.getLocalizedMessage(),e);
            return "error";
        }
    }


    @PostMapping("/registertype/single")
    public String registerTypeSingle(@RequestParam("tableName") String tableName) throws ClassNotFoundException, FileNotFoundException {
        logger.info("Entering into -> registerTypeSingle");
        logger.info("params received : tableName=" + tableName + ", ddlAndPropertiesBasePath=" + ddlAndPropertiesBasePath + ",spaceName=" + spaceName);
        try{
            String response = objectService.registerObject(tableName);
            logger.info("Exiting from -> registerTypeSingle response"+response);
            return response;
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in registerTypeSingle -> "+e.getLocalizedMessage(),e);
            return "error";
        }
    }


    @PostMapping("/registertype/sandbox")
    public String registerTypeSandbox(@RequestParam("tableName") String tableName
            ,@RequestParam("spaceName") String sandboxSpace
            ,@RequestParam("reportFilePath") String reportFilePath) {
        logger.info("start -- registertype sandbox");
        logger.info("params received : tableName=" + tableName + ", spaceName=" + sandboxSpace);
        logger.info("tableName" + tableName);
        logger.info("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        try {
            objectService.registerAndValidateInSandbox(tableName, sandboxSpace,reportFilePath);
            return "success";
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in -> registerTypeSandbox",e);
            return "error";
        }
    }
}
