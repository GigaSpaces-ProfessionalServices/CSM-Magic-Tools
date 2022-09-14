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
    public String registerTypeBatch() {
        logger.info("start -- registertype  batch");

        try{
            objectService.registerObjectBatch();
            return "success";
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in registerTypeBatch -> "+e.getLocalizedMessage(),e);
            return "error";
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
    public String registerTypeSandbox(@RequestParam("tableName") String tableName,@RequestParam("spaceName") String sandboxSpace) {
        logger.info("start -- registertype sandbox");
        logger.info("params received : tableName=" + tableName + ", spaceName=" + sandboxSpace);
        logger.info("tableName" + tableName);
        logger.info("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        try {
            objectService.registerInSandbox(tableName, sandboxSpace);
            return "success";
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error in -> registerTypeSandbox",e);
            return "error";
        }
    }

    @PostMapping("/validate")
    private String validate(@RequestParam("ddlFileName") String ddlFileName, @RequestParam("reportFilePath") String reportFilePath) throws IOException {

        String ddlFilePath = ddlAndPropertiesBasePath+"/"+ddlFileName;
        String ddl = CommonUtil.readDDLFromfile(ddlFilePath);
        //Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        //gigaSpace = admin.getSpaces().waitFor(spaceName).getGigaSpace();

        //1. Parse DDL
        Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuilders = parser.parse(ddl, "");
        Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuildersCached = parser.parse(ddl, TYPE_DISTINGUISHER_SUFFIX);

        //2. Register type (two identical ones)
        // a. No criteria
        // b. criteria all

        registerTypes(typeDescriptorBuilders, lookupLocator, lookupGroup, spaceName);
        registerTypesWithTieredStorageCriteria(typeDescriptorBuildersCached, lookupLocator, lookupGroup, spaceName);


        //3. Write records
        writeRecords(lookupLocator, lookupGroup, spaceName);

        //4. Read back, compare
        readRecords(lookupLocator, lookupGroup, spaceName);

        //5. Report
        ReportWriter rw = new ReportWriter();

        String reportStr = rw.produceReport(reportData, baseTypeDescriptorMap);
        String reportAdditionalData = rw.getAdditionalInfo();

        System.out.println("Generating report to " + reportFilePath);

        try {
            FileWriter fw = new FileWriter(reportFilePath);

            fw.write(reportStr);
            fw.write(reportAdditionalData);
            fw.close();
            logger.info("Done writing report");
            return reportFilePath;

        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception writing report", e);
            logger.debug(" Report ");
            logger.debug(reportStr);
            logger.debug(reportAdditionalData);

        }
        return null;
    }

    private void registerTypes(Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuilders, String lookupLocator, String lookupGroup, String spaceName) {
       /*  Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
       GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();*/

        logger.info("gigaSpace: " + gigaSpace);
        for (SpaceTypeDescriptorBuilder builder : typeDescriptorBuilders) {
            builder.supportsDynamicProperties(false);

            SpaceTypeDescriptor spaceTypeDescriptor = builder.create();
            baseTypeDescriptorMap.put(spaceTypeDescriptor.getTypeName(), spaceTypeDescriptor);

            gigaSpace.getTypeManager().registerTypeDescriptor(spaceTypeDescriptor);
            System.out.println("######## Successfully Register type - " + spaceTypeDescriptor.getTypeName() + " ########");
        }
    }


    private void registerTypesWithTieredStorageCriteria(Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuilders, String lookupLocator, String lookupGroup, String spaceName) {
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

        logger.info("gigaSpace: " + gigaSpace);*/
        Iterator<SpaceTypeDescriptor> typeDescriptorIterator = baseTypeDescriptorMap.values().iterator();
        for (SpaceTypeDescriptorBuilder builder : typeDescriptorBuilders) {

            builder.supportsDynamicProperties(false);
            for(String propName : typeDescriptorIterator.next().getPropertiesNames()){
                logger.info("&&&&&&&&&&&&&&propName&&&&&&&&&&--------------------------"+propName);
            }

            if (typeDescriptorIterator.hasNext()) {
                SpaceTypeDescriptor tempDescriptor = typeDescriptorIterator.next();
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(tempDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX)
                        .setCriteria("all")
                );
            } else {
                System.out.println("Warning: could not find typedescriptor name - problem setting tiered storage config.");
                SpaceTypeDescriptor tempDescriptor = builder.create();
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(tempDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX)
                        .setCriteria("all"));
            }
            SpaceTypeDescriptor spaceTypeDescriptor = builder.create();
            suffixedTypeDescriptorMap.put(spaceTypeDescriptor.getTypeName(), spaceTypeDescriptor);
            gigaSpace.getTypeManager().registerTypeDescriptor(spaceTypeDescriptor);
            System.out.println("######## Successfully Register type - " + spaceTypeDescriptor.getTypeName() + " ########");
        }
    }

    private void writeRecords(String lookupLocator, String lookupGroup, String spaceName) {
       /* Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

        logger.info("gigaSpace: " + gigaSpace);*/

        for (SpaceTypeDescriptor typeDescriptor : baseTypeDescriptorMap.values()) {
            String[] propertyNames = typeDescriptor.getPropertiesNames();
            SpaceDocument doc1 = new SpaceDocument(typeDescriptor.getTypeName());
            SpaceDocument doc2 = new SpaceDocument(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);


            //Reporting
            TableOutcome tableForDoc1 = new TableOutcome(typeDescriptor.getTypeName());
            TableOutcome tableForDoc2 = new TableOutcome(typeDescriptor.getTypeName());
            RecordOutcome recordForDoc1;
            RecordOutcome recordForDoc2;

            //TODO:  Support for composite id property
            List<String> idPropertiesNames = typeDescriptor.getIdPropertiesNames();
            String idPropertyName = idPropertiesNames.get(0);
            SpacePropertyDescriptor idProperty = typeDescriptor.getFixedProperty(idPropertyName);
            Object idPropertyValue = getPropertyValue(0, idProperty.getType());

            //TODO: Remove later when more co-ordinated interaciton supported
            //First, clear space
            gigaSpace.clear(new Object());
            for (int i = 1; i <= NUMBER_OF_RECORDS; i++) {
                recordForDoc1 = new RecordOutcome();
                recordForDoc2 = new RecordOutcome();


                for (String propertyName : propertyNames) {

                    SpacePropertyDescriptor fixedProperty = typeDescriptor.getFixedProperty(propertyName);

                    Object propertyValue = getPropertyValue(i, fixedProperty.getType());
                    if (fixedProperty.getName().equals(idPropertyName)) {
                        idPropertyValue = propertyValue;
                    }
                    doc1.setProperty(propertyName, propertyValue);
                    recordForDoc1.setWriteResult(propertyName, true);
                    doc2.setProperty(propertyName, propertyValue);
                    recordForDoc2.setWriteResult(propertyName, true);
                }
                recordForDoc1.setIdColumn(idPropertyValue);
                recordForDoc2.setIdColumn(idPropertyValue);

                try {
                    gigaSpace.write(doc1);
                    recordForDoc1.setRecordWriteResult(true);
                } catch (DataAccessException dataAccessException) {
                    recordForDoc1.setRecordWriteResult(false);
                    recordForDoc1.setRecordWriteException(dataAccessException);
                } catch (Exception ex) {
                    recordForDoc1.setRecordWriteResult(false);
                    recordForDoc1.setRecordWriteException(ex);
                }

                try {
                    gigaSpace.write(doc2);
                    recordForDoc2.setRecordWriteResult(true);
                } catch (DataAccessException dataAccessException) {
                    recordForDoc2.setRecordWriteResult(false);
                    recordForDoc2.setRecordWriteException(dataAccessException);
                } catch (Exception ex) {
                    recordForDoc2.setRecordWriteResult(false);
                    recordForDoc2.setRecordWriteException(ex);
                }
                tableForDoc1.addRecord(recordForDoc1);
                tableForDoc2.addRecord(recordForDoc2);
            }
            reportData.addTsTable(typeDescriptor.getTypeName(), tableForDoc1);
            reportData.addCachedTable(typeDescriptor.getTypeName(), tableForDoc2);

        }

    }

    private void readRecords(String lookupLocator, String lookupGroup, String spaceName) {

        System.out.println("Reading back entries written just now.");
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();*/

        for (SpaceTypeDescriptor typeDescriptor : baseTypeDescriptorMap.values()) {
            // Read:

            TableOutcome tsResults = reportData.getTsResults(typeDescriptor.getTypeName());
            TableOutcome csResults = reportData.getCsResults(typeDescriptor.getTypeName());

            SQLQuery<SpaceDocument> query =
                    new SQLQuery<SpaceDocument>(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX, "");

            SpaceDocument[] result1 = {};
            try {
                result1 = gigaSpace.readMultiple(query);
            } catch (Throwable th) {
                csResults.additionalInfo.put("Exception Reading the table :", th);
                logger.error("Exception reading from Cache : ", th);
            }

            System.out.println("Comparing records for type " + typeDescriptor.getTypeName() + " and " + typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);

            System.out.println("Differing records will be printed out");
            for (int i = 0; i < result1.length; i++) {
                String idPropertyName = typeDescriptor.getIdPropertiesNames().get(0);
                RecordOutcome recordOutcome1 = getRecordOutcome(csResults, result1[i].getProperty(idPropertyName));

                if (recordOutcome1 == null) {
                    logger.debug("Could not read records for id property " + result1[i].getProperty(idPropertyName));
                    String info = " Id property : " + idPropertyName;
                    info += " Value : " + result1[i].getProperty(idPropertyName);
                    csResults.additionalInfo.put("Could not retrieve record for Id value  " + info, result1[i].getProperty(idPropertyName));
                } else {
                    recordOutcome1.recordRead = true;
                }


                RecordOutcome recordOutcome2 = getRecordOutcome(tsResults, result1[i].getProperty(idPropertyName));
                if (recordOutcome2 == null) {
                    //recordOutcome2.recordRead = false;
                    String info = " Id property : " + idPropertyName;
                    info += " Value : " + result1[i].getProperty(idPropertyName);
                    tsResults.additionalInfo.put("Could not retrieve record for Id value  " + info, result1[i].getProperty(idPropertyName));

                } else {
                    IdQuery<SpaceDocument> spaceDocumentIdQuery = new IdQuery<SpaceDocument>(typeDescriptor.getTypeName(), result1[i].getProperty(idPropertyName));
                    SpaceDocument spaceDocument = null;
                    try {
                        spaceDocument = gigaSpace.readById(spaceDocumentIdQuery);
                    } catch (Throwable th) {
                        recordOutcome2.recordRead = false;
                        logger.debug("Exception reading  from TS for table, property " + typeDescriptor.getTypeName() + " , " + result1[i].getProperty(idPropertyName));
                        logger.error("Exception details : ", th);
                        recordOutcome2.additionalInfo.put("Exception reading record from TS ", th);
                    }
                    if (spaceDocument == null) {
                        logger.debug(" Could not find matching record for " + result1[i].getTypeName());
                        recordOutcome2.recordRead = false;

                    } else {
                        compare(result1[i], spaceDocument, recordOutcome1, recordOutcome2, lookupLocator, lookupGroup, spaceName);
                    }

                }
            }

        }

    }

    private RecordOutcome getRecordOutcome(TableOutcome results, Object idValue) {
        for (RecordOutcome recordOutcome : results.records()) {
            if (idValue.equals(recordOutcome.getIdValue())) {
                return recordOutcome;
            }
        }
        logger.info(" Could not read recorded value  ");
        return null;
    }

    private void compare(SpaceDocument doc1, SpaceDocument doc2, RecordOutcome recordOutcome1, RecordOutcome recordOutcome2, String lookupLocator, String lookupGroup, String spaceName) {
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();*/

        logger.info("gigaSpace: " + gigaSpace);
        SpaceTypeDescriptor typeDescriptor = gigaSpace.getTypeManager().getTypeDescriptor(doc1.getTypeName());
        String[] propertiesNames = typeDescriptor.getPropertiesNames();
        recordOutcome1.recordRead = true;
        recordOutcome2.recordRead = true;

        //Initialized true, if one or more fields differ, these will be set to false
        recordOutcome1.comparedEqual = true;
        recordOutcome2.comparedEqual = true;
        for (String property : propertiesNames) {
            Object val1 = doc1.getProperty(property);
            Object val2 = doc2.getProperty(property);
            if (val1 != null) {
                if (val1.equals(val2)) {
                    recordOutcome1.setFieldRead(property, true);
                    recordOutcome2.setFieldRead(property, true);

                    continue;
                } else {
                    System.out.println("--------- Records differ ");
                    System.out.println(doc1);
                    System.out.println(doc2);
                    recordOutcome1.setFieldRead(property, true);
                    recordOutcome2.setFieldRead(property, false);
                    recordOutcome1.comparedEqual = false;
                    recordOutcome2.comparedEqual = false;
                }
            } else {
                if (val2 != null) {
                    System.out.println("--------- Records differ ");
                    System.out.println(doc1);
                    System.out.println(doc2);
                    recordOutcome1.setFieldRead(property, true);
                    recordOutcome2.setFieldRead(property, false);
                    recordOutcome1.comparedEqual = false;
                    recordOutcome2.comparedEqual = false;
                } else {
                    logger.info(" Both Property values are null for property " + property);
                    recordOutcome1.setFieldRead(property, false);
                    recordOutcome2.setFieldRead(property, false);
                    recordOutcome2.recordRead = false;
                    recordOutcome1.comparedEqual = false;
                    recordOutcome2.comparedEqual = false;

                }
            }

        }
    }


}
