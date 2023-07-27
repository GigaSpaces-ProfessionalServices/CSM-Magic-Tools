package com.gigaspaces.objectManagement.controller;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.objectManagement.model.ReportData;
import com.gigaspaces.objectManagement.service.DdlParser;
import com.gigaspaces.objectManagement.service.ObjectService;
import com.gigaspaces.objectManagement.service.SearchType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ObjectController {

    private static final String TYPE_DISTINGUISHER_SUFFIX = "_C";
    private static final int NUMBER_OF_RECORDS = 10;
    Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap = new TreeMap<>();
    Map<String, SpaceTypeDescriptor> suffixedTypeDescriptorMap = new TreeMap<>();
    //ReportData reportData;
    ReportData reportData = new ReportData();
    private Logger logger = LoggerFactory.getLogger(ObjectController.class);
    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private ObjectService objectService;
    @Autowired
    private DdlParser parser;
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
    public JsonArray getObjectList() {
        logger.info("Entering into -> getObjectList");
        try {
            JsonArray jsonArray = objectService.listObjects();
            logger.info("Exiting from -> getObjectList");
            return jsonArray;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getObjectList -> " + e.getLocalizedMessage(), e);
            return null;
        }

    }

    @PostMapping("/registertype/batch")
    public JsonObject registerTypeBatch() {
        logger.info("start -- registertype  batch");

        try {
            JsonObject jsonObject = objectService.registerObjectBatch();
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in registerTypeBatch -> " + e.getLocalizedMessage(), e);
            return new JsonObject();
        }

    }

    @PostMapping("/unregistertype")
    public String unregisterType(@RequestParam("type") String type) {
        logger.info("start -- unregistertype");
        logger.info("params received :type=" + type + ",spaceName=" + spaceName);
        try {
            //1. Undeploy feeder pu for this type (Assume that pu name is starting with type name)
            String puName = type.indexOf(".")>0?type.substring(type.lastIndexOf(".")+1):type;
            puName=puName.toLowerCase();
            logger.info(" Processing unit search keyword = "+puName);
            ProcessingUnit pu = objectService.searchProcessingUnitByName(puName, SearchType.PREFIX);
            if(pu!=null) {
                logger.info("Undeploy processing unit with name = "+pu.getName());
                objectService.undeployProcessingUnit(pu);
            }else{
                logger.info("Processing unit with name="+puName+" not found");
            }

            //2. Un register type
            logger.info("Unregister space type with name = "+type);
            objectService.unregisterObject(type);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in unregisterType -> " + e.getLocalizedMessage(), e);
            return "error";
        }
    }

    @PostMapping("/registertype/single")
    public String registerTypeSingle(@RequestParam("tableName") String tableName) {
        logger.info("Entering into -> registerTypeSingle");
        logger.info(
                "params received : tableName=" + tableName + ", ddlAndPropertiesBasePath=" + ddlAndPropertiesBasePath
                        + ",spaceName=" + spaceName);
        try {
            String response = objectService.registerObject(tableName);
            logger.info("Exiting from -> registerTypeSingle response" + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in registerTypeSingle -> " + e.getLocalizedMessage(), e);
            return "error";
        }
    }

    @PostMapping("/registertype/sandbox")
    public String registerTypeSandbox(
            @RequestParam("tableName") String tableName
            , @RequestParam("spaceName") String sandboxSpace
            , @RequestParam("reportFilePath") String reportFilePath) {
        logger.info("start -- registertype sandbox");
        logger.info("params received : tableName=" + tableName + ", spaceName=" + sandboxSpace);
        logger.info("tableName" + tableName);
        logger.info("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        try {
            objectService.registerAndValidateInSandbox(tableName, sandboxSpace, reportFilePath);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in -> registerTypeSandbox", e);
            return "error";
        }
    }

    @GetMapping("/inmemory/count")
    public int getObjectCount(
            @RequestParam("objectType") String objectType,
            @RequestParam("condition") String condition) {
        logger.info("Entering into -> getObjectCount,  objectType: " + objectType + ", condition: " + condition);
        try {
            int response = objectService.objectCountInMemory(objectType, condition);
            logger.info("Exiting from -> getObjectCount : " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getObjectCount -> " + e.getLocalizedMessage(), e);
        }
        return 0;
    }

    @GetMapping("/inmemory/minmax")
    public SpaceDocument getObjectMinMax(
            @RequestParam("objectType") String objectType,
            @RequestParam("minColName") String minColName,
            @RequestParam("maxColName") String maxColName) {
        logger.info("Entering into -> getObjectCount,  objectType: " + objectType + ", minColName: " + minColName
                + ", maxColName: " + maxColName);
        try {
            SpaceDocument response = objectService.objectMaxMinInMemory(
                    objectType,
                    minColName,
                    maxColName);
            logger.info("Exiting from -> getObjectMinMax : " + response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getObjectCount -> " + e.getLocalizedMessage(), e);
        }
        return null;
    }
}
