package com.gigaspaces.objectManagement.controller;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.metadata.PropertyInfo;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.objectManagement.model.RecordOutcome;
import com.gigaspaces.objectManagement.model.ReportData;
import com.gigaspaces.objectManagement.model.SpaceObjectDto;
import com.gigaspaces.objectManagement.model.TableOutcome;
import com.gigaspaces.objectManagement.service.DdlParser;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.gigaspaces.objectManagement.utils.ReportWriter;
import com.gigaspaces.query.IdQuery;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gigaspaces.objectManagement.utils.DataGeneratorUtils.getPropertyValue;
import static java.nio.file.Files.readAllBytes;

@RestController
public class ObjectController {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private GigaSpace gigaSpace;
    @Autowired
    private DdlParser parser;
    private static final String TYPE_DISTINGUISHER_SUFFIX = "_C";
    private static final int NUMBER_OF_RECORDS = 10;
    Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap = new TreeMap<>();
    Map<String, SpaceTypeDescriptor> suffixedTypeDescriptorMap = new TreeMap<>();
    ReportData reportData = new ReportData();

    @GetMapping("/list")
    public JsonArray getObjectList(@RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) {
        logger.info("start -- list ");
        logger.info("params received : lookupGroup=" + lookupGroup + ",lookupLocator=" + lookupLocator + ", isSecured=" + isSecured);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        if (isSecured != null && isSecured) {
            if (username == null || password == null || "".equals(username) || "".equals(password)) {
                logger.severe("username " + username + "password" + " is not proper");
            }
        }
        Admin admin;
        if (isSecured != null && isSecured) {
            admin = new AdminFactory().addLocator(lookupLocator).credentials(username, password).addGroups(lookupGroup).createAdmin();
        } else {
            admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        }

        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;

        //admin.getGridServiceAgents().waitForAtLeastOne();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            jsonObject = new JsonObject();
            logger.info("Space name:" + space.getUid());
            String spaceName = space.getName();
            if (spaceName != null && spaceName.equals("")) {
                spaceName = space.getUid(); // Assign first space if it is blank
            }
            jsonObject.addProperty("spacename", spaceName);
            if (spaceName != null && spaceName.equals(space.getUid())) {
                String[] strings = space.getInstances()[0].getRuntimeDetails().getClassNames();
                Map<String, ITypeDesc> classDescriptors = space.getInstances()[0].getRuntimeDetails().getClassDescriptors();
                List<SpaceObjectDto> spaceObjectDto = new ArrayList<>();
                JsonObject jsonObject3;
                JsonArray jsonArray3 = new JsonArray();
                for (Map.Entry<String, ITypeDesc> entry : classDescriptors.entrySet()) {
                    if (entry.getKey().equals("java.lang.Object")) continue;
                    JsonArray jsonArray2 = new JsonArray();
                    jsonObject3 = new JsonObject();
                    jsonObject3.addProperty("tablename", entry.getKey());
                    logger.info(entry.getKey() + " " + entry.getValue());
                    for (PropertyInfo pi : entry.getValue().getProperties()) {
                        JsonObject jsonObject2 = new JsonObject();
                        SpaceObjectDto spaceObject = new SpaceObjectDto();
                        jsonObject2.addProperty("columnname", pi.getName());
                        jsonObject2.addProperty("columntype", pi.getType().getTypeName());
                        jsonObject2.addProperty("isSpacePrimitive", String.valueOf(pi.isSpacePrimitive()));

                        spaceObject.setObjName(pi.getName());
                        spaceObject.setObjtype(String.valueOf(pi.getType()));
                        spaceObject.setSpaceId(String.valueOf(pi.isSpacePrimitive()));
                        spaceObject.setSpaceName(spaceName);
                        logger.info(pi.getName() + " -> " + pi.getType());
                        spaceObjectDto.add(spaceObject);
                        jsonArray2.add(jsonObject2);
                    }
                    jsonObject3.add("columns", jsonArray2);
                    jsonArray3.add(jsonObject3);
                }
                jsonObject.add("objects", jsonArray3);
                for (String className : strings) {
                    if (className.equals("java.lang.Object")) continue;
                    logger.info("className: " + className);
                }
            }
            jsonArray.add(jsonObject);
        }
        logger.info("end -- list ");
        return jsonArray;
    }

    @PostMapping("/registertype/batch")
    public String registerTypeBatch(@RequestParam("tableListfilePath") String tableListfilePath, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("spaceName") String spaceName, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) throws ClassNotFoundException, FileNotFoundException {
        logger.info("start -- registertype  batch");
        logger.info("params received : lookupGroup=" + lookupGroup + ",lookupLocator=" + lookupLocator + ",tableListfilePath=" + tableListfilePath + ", ddlAndPropertiesBasePath=" + ddlAndPropertiesBasePath + ",spaceName=" + spaceName);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        if (isSecured != null && isSecured) {
            if (username == null || password == null || "".equals(username) || "".equals(password)) {
                logger.severe("username " + username + "password" + " is not proper");
            }
        }
        Admin admin;
        if (isSecured != null && isSecured) {
            admin = new AdminFactory().addLocator(lookupLocator).credentials(username, password).addGroups(lookupGroup).createAdmin();
        } else {
            admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        }

        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        logger.info("gigaSpace: " + gigaSpace);
        String tablesList = null;
        try (BufferedReader br = new BufferedReader(new FileReader(new File(tableListfilePath)))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (tablesList == null) {
                    tablesList = line;
                } else {
                    tablesList = tablesList + line;
                }
            }
        } catch (
                FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] tablesListArray = tablesList.split("\\,", -1);
        for (String table : tablesListArray) {
            if (!ddlAndPropertiesBasePath.endsWith("/")) {
                ddlAndPropertiesBasePath += "/";
            }
            Properties properties = CommonUtil.readProperties(ddlAndPropertiesBasePath + table);
            String spaceId = properties.getProperty("spaceId");
            String spaceIdType = properties.getProperty("spaceIdType");
            String routing = properties.getProperty("routing");
            String index = properties.getProperty("index");
            String indexType = properties.getProperty("indexType");
            String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

            // DdlParser parser = new DdlParser();
            Collection<SpaceTypeDescriptorBuilder> result = null;

            try {
                result = parser.parse(Paths.get(ddlAndPropertiesBasePath + table + ".ddl"));

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result != null) {
                logger.info("Number of DDLs: " + result.size());

                for (SpaceTypeDescriptorBuilder builder : result) {

                    CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                    CommonUtil.addRouting(routing, builder);
                    CommonUtil.addIndex(index, indexType, builder);
                    CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                    SpaceTypeDescriptor typeDescriptor = builder.create();
                    CommonUtil.registerType(typeDescriptor, gigaSpace);
                }
            } else {
                logger.info("Number of DDLs: null");
            }
        }
        logger.info("end -- registertype  batch");
        return "Registered";
    }

    @PostMapping("/unregistertype")
    public String unregisterType(@RequestParam("spaceName") String spaceName, @RequestParam("type") String type, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) {
        logger.info("start -- unregistertype");
        logger.info("params received : lookupGroup=" + lookupGroup + ",lookupLocator=" + lookupLocator + ",type=" + type + ",spaceName=" + spaceName);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        if (isSecured != null && isSecured) {
            if (username == null || password == null || "".equals(username) || "".equals(password)) {
                logger.severe("username " + username + "password" + " is not proper");
            }
        }
        Admin admin;
        if (isSecured != null && isSecured) {
            admin = new AdminFactory().addLocator(lookupLocator).credentials(username, password).addGroups(lookupGroup).createAdmin();
        } else {
            admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        }
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        logger.info("gigaSpace: " + gigaSpace);
        CommonUtil.unregisterType(type, gigaSpace);
        logger.info("end -- unregistertype");
        return "Unregistered";
    }


    @PostMapping("/registertype/single")
    public String registerTypeSingle(@RequestParam("tableName") String tableName, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("spaceName") String spaceName, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) throws ClassNotFoundException, FileNotFoundException {
        logger.info("start -- registertype single");
        logger.info("params received : lookupGroup=" + lookupGroup + ",lookupLocator=" + lookupLocator + ",tableName=" + tableName + ", ddlAndPropertiesBasePath=" + ddlAndPropertiesBasePath + ",spaceName=" + spaceName);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        if (isSecured != null && isSecured) {
            if (username == null || password == null || "".equals(username) || "".equals(password)) {
                logger.severe("username " + username + "password" + " is not proper");
            }
        }
        Admin admin;
        if (isSecured != null && isSecured) {
            admin = new AdminFactory().addLocator(lookupLocator).credentials(username, password).addGroups(lookupGroup).createAdmin();
        } else {
            admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        }
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        logger.info("gigaSpace: " + gigaSpace);

        if (!ddlAndPropertiesBasePath.endsWith("/")) {
            ddlAndPropertiesBasePath += "/";
        }
        Properties properties = CommonUtil.readProperties(ddlAndPropertiesBasePath + tableName);
        String spaceId = properties.getProperty("spaceId");
        String spaceIdType = properties.getProperty("spaceIdType");
        String routing = properties.getProperty("routing");
        String index = properties.getProperty("index");
        String indexType = properties.getProperty("indexType");
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;

        try {
            result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
            }
        } else {
            logger.info("Number of DDLs: null");
        }
        logger.info("end -- registertype single");
        return "Registered";
    }


    @PostMapping("/registertype/sandbox")
    public String registerTypeSandbox(@RequestParam("tableName") String tableName, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) throws ClassNotFoundException, FileNotFoundException {
        logger.info("start -- registertype sandbox");
        logger.info("params received : lookupGroup=" + lookupGroup + ",lookupLocator=" + lookupLocator + ",tableName=" + tableName + ", ddlAndPropertiesBasePath=" + ddlAndPropertiesBasePath);
        String spaceName = "sandboxSpace";
        logger.info("tableName" + tableName);
        logger.info("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        if (isSecured != null && isSecured) {
            if (username == null || password == null || "".equals(username) || "".equals(password)) {
                logger.severe("username " + username + "password" + " is not proper");
            }
        }
        Admin admin;
        if (isSecured != null && isSecured) {
            admin = new AdminFactory().addLocator(lookupLocator).credentials(username, password).addGroups(lookupGroup).createAdmin();
        } else {
            admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        }
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();

       /* AbstractSpaceConfigurer configurer = embedded ? new EmbeddedSpaceConfigurer(spaceName)
                .addProperty("space-config.QueryProcessor.datetime_format", "yyyy-MM-dd HH:mm:ss.SSS")
               .tieredStorage(new TieredStorageConfigurer().addTable(new TieredStorageTableConfig().setName(MyPojo.class.getName()).setCriteria("age > 20")))
                : new SpaceProxyConfigurer(spaceName).lookupGroups("yohanaPC");*/
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

        logger.info("gigaSpace: " + gigaSpace);

        if (!ddlAndPropertiesBasePath.endsWith("/")) {
            ddlAndPropertiesBasePath += "/";
        }
        Properties properties = CommonUtil.readProperties(ddlAndPropertiesBasePath + tableName);
        String spaceId = properties.getProperty("spaceId");
        String spaceIdType = properties.getProperty("spaceIdType");
        String routing = properties.getProperty("routing");
        String index = properties.getProperty("index");
        String indexType = properties.getProperty("indexType");
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;

        try {
            result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
                logger.info("Registered object");
            }
        } else {
            logger.info("Number of DDLs: null");
        }
        logger.info("Unregistering object");
        CommonUtil.unregisterType(tableName, gigaSpace);
        logger.info("Unregistered object");
        logger.info("Removing PU");
        pu.undeploy();
        logger.info("Removed PU");
        logger.info("end -- registertype sandbox");
        return "Done";
    }

    //@RequestParam("tableListfilePath") String tableListfilePath, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath,
    // @RequestParam("spaceName") String spaceName, @RequestParam("lookupGroup") String lookupGroup,
    // @RequestParam("lookupLocator") String lookupLocator
    @PostMapping("/registertype/validate")
    private String validate(@RequestParam("ddlAndPropertiesBasePath") String ddl, @RequestParam("reportFilePath") String reportFilePath,
                            @RequestParam("lookupLocator") String lookupLocator, @RequestParam("lookupGroup") String lookupGroup,
                            @RequestParam("spaceName") String spaceName, @RequestParam("isSecured") Boolean isSecured, @RequestParam("username") String username, @RequestParam("password") String password) throws IOException {
        ddl = readDDLFromfile(ddl);
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        gigaSpace = admin.getSpaces().waitFor(spaceName).getGigaSpace();

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
            logger.log(Level.SEVERE, "Exception writing report", e);
            logger.warning(" Report ");
            logger.warning(reportStr);
            logger.warning(reportAdditionalData);

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
                logger.log(Level.WARNING, "Exception reading from Cache : ", th);
            }

            System.out.println("Comparing records for type " + typeDescriptor.getTypeName() + " and " + typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);

            System.out.println("Differing records will be printed out");
            for (int i = 0; i < result1.length; i++) {
                String idPropertyName = typeDescriptor.getIdPropertiesNames().get(0);
                RecordOutcome recordOutcome1 = getRecordOutcome(csResults, result1[i].getProperty(idPropertyName));

                if (recordOutcome1 == null) {
                    logger.warning("Could not read records for id property " + result1[i].getProperty(idPropertyName));
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
                        logger.warning("Exception reading  from TS for table, property " + typeDescriptor.getTypeName() + " , " + result1[i].getProperty(idPropertyName));
                        logger.log(Level.WARNING, "Exception details : ", th);
                        recordOutcome2.additionalInfo.put("Exception reading record from TS ", th);
                    }
                    if (spaceDocument == null) {
                        logger.warning(" Could not find matching record for " + result1[i].getTypeName());
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
        logger.warning(" Could not read recorded value  ");
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

    private static String readDDLFromfile(String ddlFileName) throws IOException {
        String ddlText = new String(readAllBytes(Paths.get(ddlFileName)));
        return ddlText;
    }
}
