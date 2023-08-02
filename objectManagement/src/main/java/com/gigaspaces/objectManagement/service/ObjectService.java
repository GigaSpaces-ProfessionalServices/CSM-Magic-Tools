package com.gigaspaces.objectManagement.service;

import com.gigaspaces.admin.quiesce.QuiesceState;
import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.client.CountModifiers;
import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageConfig;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.AddTypeIndexesResult;
import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexFactory;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gigaspaces.objectManagement.model.RecordOutcome;
import com.gigaspaces.objectManagement.model.ReportData;
import com.gigaspaces.objectManagement.model.SpaceObjectDto;
import com.gigaspaces.objectManagement.model.TableOutcome;
import com.gigaspaces.objectManagement.utils.CommonUtil;

import static com.gigaspaces.objectManagement.utils.CommonUtil.readAdaptersProperties;
import static com.gigaspaces.objectManagement.utils.DataGeneratorUtils.getPropertyValue;
import com.gigaspaces.objectManagement.utils.ReportWriter;
import com.gigaspaces.query.IdQuery;
import com.gigaspaces.query.aggregators.AggregationResult;
import com.gigaspaces.query.aggregators.AggregationSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.admin.IRemoteJSpaceAdmin;
import com.j_spaces.core.client.SQLQuery;
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

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.quiesce.QuiesceRequest;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class ObjectService {

    private static final int NUMBER_OF_RECORDS = 10;
    private static final String TYPE_DISTINGUISHER_SUFFIX = "_C";
    @Value("${safe.appid}")
    String appId;
    @Value("${safe.safeid}")
    String safeId;
    @Value("${safe.objectid}")
    String objectId;
    ReportData reportData = new ReportData();
    private Logger logger = LoggerFactory.getLogger(ObjectService.class);
    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private DdlParser parser;
    @Autowired
    private CommonUtil commonUtil;
    @Value("${ddl.properties.file.path}")
    private String ddlAndPropertiesBasePath;
    @Value("${space.name}")
    private String spaceName;
    @Value("${table.batch.file.path}")
    private String tableListFilePath;
    @Value("${tier.criteria.file}")
    private String strTierCriteriaFile;

    @Value("${adapter.property.file}")
    private String adaptersFilePath;

    @Value("${lookup.group}")
    private String lookupGroup;
    @Value("${lookup.locator}")
    private String lookupLocator;
    @Value("${odsx.profile}")
    private String odsxProfile;
    @Value("${gs.username}")
    private String gsUsername;
    @Value("${gs.password}")
    private String gsPassword;

    public String registerObject(String tableName) throws ClassNotFoundException, FileNotFoundException, Exception {
        //Admin admin = commonUtil.getAdmin();
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }*/
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
        String broadcast = properties.getProperty("braodcast");
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");
        Properties adaptersProperties = readAdaptersProperties(adaptersFilePath);

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;
        parser.setAdaptersProperties(adaptersProperties);
        try {
            result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));

        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
        if (result != null) {
            logger.info("gigaSpace.getTypeManager().getTypeDescriptor(tableName) : " + gigaSpace.getTypeManager()
                    .getTypeDescriptor(tableName));
            if (gigaSpace.getTypeManager().getTypeDescriptor(tableName) != null) {
                return "duplicate";
            }
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, broadcast, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                builder = CommonUtil.setTierCriteria(tableName, builder, strTierCriteriaFile);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
            }
        } else {
            logger.info("Number of DDLs: null");
        }
        logger.info("end -- registertype single");
        return "success";
    }

    public JsonArray listObjects() throws Exception {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;
        if (!ddlAndPropertiesBasePath.endsWith("/")) {
            ddlAndPropertiesBasePath += "/";
        }
        IRemoteJSpaceAdmin remoteAdmin =
                (IRemoteJSpaceAdmin) gigaSpace.getSpace().getAdmin();

        List<String> classList = remoteAdmin.getRuntimeInfo().m_ClassNames;
      /*  List<Integer> countTypesInMemory = remoteAdmin.getRuntimeInfo().m_RamNumOFEntries;
        logger.info(">>>> remoteAdmin.getRuntimeInfo().m_RamNumOFEntries " + remoteAdmin.getRuntimeInfo()
        .m_RamNumOFEntries);
        logger.info(">>>> remoteAdmin.getRuntimeInfo().m_NumOFEntries " + remoteAdmin.getRuntimeInfo().m_NumOFEntries);
        logger.info(">>>> remoteAdmin.getRuntimeInfo().m_NumOFTemplates " + remoteAdmin.getRuntimeInfo()
        .m_NumOFTemplates);
        logger.info(">>>> remoteAdmin.getSpaceInstanceRemoteClassLoaderInfo " + remoteAdmin
        .getSpaceInstanceRemoteClassLoaderInfo());*/
        TieredStorageConfig tieredStorageConfig = remoteAdmin.getRuntimeInfo().getTieredStorageConfig();
        logger.info(">>>> tieredStorageConfig : " + tieredStorageConfig);
        List<SpaceObjectDto> spaceObjectDto = new ArrayList<>();
        GigaSpaceTypeManager gigaSpaceTypeManager = gigaSpace.getTypeManager();
        //((IRemoteJSpaceAdmin) gigaSpace.getSpace().getAdmin()).getName()
        jsonObject = new JsonObject();
        jsonObject.addProperty("spacename", gigaSpace.getSpaceName());
        JsonObject jsonObject3;
        JsonArray jsonArray3 = new JsonArray();
        for (int a = 0; a < classList.size(); a++) {
            //for (Object obj : classList) {
            String objectType = classList.get(a);
            //  int objectCount = countTypesInMemory.get(a);
            if (objectType.equals("java.lang.Object")) {
                continue;
            }
            JsonArray jsonArray2 = new JsonArray();
            jsonObject3 = new JsonObject();
            jsonObject3.addProperty("tablename", objectType);
            String objPropFile = objectType;
            if (objectType.endsWith("_C")) {
                objPropFile = objectType.substring(0, objectType.lastIndexOf("_"));
            }
            String spaceId = "";
            String routing = "";
            String index = "";
            // gigaSpaceTypeManager.getTypeDescriptor("a").getTieredStorageTableConfig().getCriteria();
            logger.info("ddlAndPropertiesBasePath + objPropFile : " + ddlAndPropertiesBasePath + objPropFile);
            if (new File(ddlAndPropertiesBasePath + objPropFile).exists()) {
                Properties properties = CommonUtil.readProperties(ddlAndPropertiesBasePath + objPropFile);
                spaceId = properties.getProperty("spaceId") != null ? properties.getProperty("spaceId") : "";
                routing = properties.getProperty("routing") != null ? properties.getProperty("routing") : "";
                index = properties.getProperty("index") != null ? properties.getProperty("index") : "";
            }
            //  String criteria = CommonUtil.getTierCriteriaConfig(objectType, strTierCriteriaFile);

            jsonObject3.addProperty("spaceId", spaceId);
            //jsonObject3.addProperty("objectInMemory", gigaSpace.count(objectType, CountModifiers.MEMORY_ONLY_SEARCH));
            //not required we can take from runtimeinfo
          /*  try {
                SQLQuery<SpaceDocument> query = new SQLQuery<>(objectType,"");
                logger.info(">>>>>>>>>>>objectType + count "+ gigaSpace.count(query, CountModifiers
                .MEMORY_ONLY_SEARCH));
            } catch (Exception e){
                logger.info(">>>>>objectType "+objectType+"======="+e.getLocalizedMessage(),e);
            }*/
            //  logger.info(">>>>>>>>>>>objectType " + objectType + ", count " + objectCount);
            //  jsonObject3.addProperty("objectInMemory", objectCount);
            jsonObject3.addProperty("routing", routing);
            jsonObject3.addProperty("index", index);
            //jsonObject3.addProperty("criteria", criteria != null && criteria.trim() != "" ? criteria : "");

            logger.info("####################");
            logger.info(">>>> tieredStorageConfig objectType : " + objectType + ", tieredStorageConfig"
                    + (tieredStorageConfig!=null ? tieredStorageConfig.getTable(objectType) : tieredStorageConfig));

            SpaceTypeDescriptor spaceTypeDescriptor = gigaSpaceTypeManager.getTypeDescriptor(objectType);

            String[] propertiesName = spaceTypeDescriptor.getPropertiesNames();
            String[] propertiesType = spaceTypeDescriptor.getPropertiesTypes();

            List<String> spaceIdProp = spaceTypeDescriptor.getIdPropertiesNames();
            logger.info("spaceIdProp =>" + spaceIdProp);
            List<String> idPropsList = spaceTypeDescriptor.getIdPropertiesNames();
            //List<String> spaceIdList = spaceTypeDescriptor.getIdPropertiesNames();
            String routingPropertyName = spaceTypeDescriptor.getRoutingPropertyName();
            Map<String, SpaceIndex> indexesMap = spaceTypeDescriptor.getIndexes();
            String tiercriteria = "";
            String criteriaFieldname = "";
            logger.info(">>>>spaceTypeDescriptor.getTieredStorageTableConfig() :"
                    + spaceTypeDescriptor.getTieredStorageTableConfig());
            if (tieredStorageConfig!=null && tieredStorageConfig.getTable(objectType) != null && !tieredStorageConfig.getTable(objectType).isTransient()) {
                tiercriteria = tieredStorageConfig.getTable(objectType).getCriteria();
                criteriaFieldname = tieredStorageConfig.getTable(objectType).getName();
                if (tiercriteria == null) {
                    tiercriteria = tieredStorageConfig.getTable(objectType).getPeriod().toString();
                    criteriaFieldname = tieredStorageConfig.getTable(objectType).getTimeColumn();
                } else {
                    if (tiercriteria.split("<").length > 1) {
                        criteriaFieldname = tiercriteria.split("<")[0];
                    } else if (tiercriteria.split(">").length > 1) {
                        criteriaFieldname = tiercriteria.split(">")[0];
                    } else if (tiercriteria.split("=").length > 1) {
                        criteriaFieldname = tiercriteria.split("=")[0];
                    }
                }

            }
            for (int i = 0; i < propertiesName.length; i++) {
                //String prop = propertiesName[i];
                //SpacePropertyDescriptor propertyDescriptor = spaceTypeDescriptor.getFixedProperty(prop);
                //spaceTypeDescriptor
                //System.out.println("  Name:" + propertyDescriptor.getName() + " Type:" + propertyDescriptor
                // .getTypeName() + " Storage Type:"
                //       + propertyDescriptor.getStorageType());
                JsonObject jsonObject2 = new JsonObject();
                //     SpaceObjectDto spaceObject = new SpaceObjectDto();

                jsonObject2.addProperty("columnname", propertiesName[i]);
                jsonObject2.addProperty("columntype", propertiesType[i]);

                jsonObject2.addProperty("spaceId", idPropsList.contains(propertiesName[i]) ? "Yes" : "");
                jsonObject2.addProperty("spaceRouting", propertiesName[i].equals(routingPropertyName) ? "Yes" : "");
                jsonObject2.addProperty(
                        "spaceIndex",
                        indexesMap.containsKey(propertiesName[i]) ? indexesMap.get(propertiesName[i])
                                .getIndexType()
                                .name() : "");
                jsonObject2.addProperty(
                        "tierCriteria",
                        propertiesName[i].equals(criteriaFieldname) ? tiercriteria : "");
                //                jsonObject2.addProperty("isSpacePrimitive", idPropsList.toString());

              /*  spaceObject.setObjName(propertyDescriptor.getName());
                spaceObject.setObjtype(String.valueOf(propertyDescriptor.getTypeName()));
                spaceObject.setSpaceId(idPropsList.toString());
                spaceObject.setSpaceName(spaceName);*/
                logger.info(propertiesName[i] + " -> " + propertiesType[i]);
                //spaceObjectDto.add(spaceObject);
                jsonArray2.add(jsonObject2);
            }

            jsonObject3.add("columns", jsonArray2);
            jsonArray3.add(jsonObject3);
        }
        jsonObject.add("objects", jsonArray3);
        jsonArray.add(jsonObject);

        logger.info("end -- list ");
        return jsonArray;
    }

    public JsonObject registerObjectBatch() throws FileNotFoundException, Exception {
        //Admin admin = commonUtil.getAdmin();
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }*/
        JsonObject jsonObject = new JsonObject();
        logger.info("gigaSpace: " + gigaSpace);
        String tablesList = null;
        try (BufferedReader br = new BufferedReader(new FileReader(new File(tableListFilePath)))) {
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
            String broadcast = properties.getProperty("braodcast");
            String supportDynamicProperties = properties.getProperty("supportDynamicProperties");
            Properties adaptersProperties = readAdaptersProperties(adaptersFilePath);

            // DdlParser parser = new DdlParser();
            Collection<SpaceTypeDescriptorBuilder> result = null;
            parser.setAdaptersProperties(adaptersProperties);
            try {
                result = parser.parse(Paths.get(ddlAndPropertiesBasePath + table + ".ddl"));

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result != null) {
                if (gigaSpace.getTypeManager().getTypeDescriptor(table) != null) {
                    jsonObject.addProperty(table, "Already exist so not registered");
                    continue;
                }

                logger.info("Number of DDLs: " + result.size());

                for (SpaceTypeDescriptorBuilder builder : result) {

                    CommonUtil.addSpaceId(spaceId, spaceIdType, broadcast, builder);
                    CommonUtil.addRouting(routing, builder);
                    CommonUtil.addIndex(index, indexType, builder);
                    CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                    builder = CommonUtil.setTierCriteria(table, builder, strTierCriteriaFile);
                    SpaceTypeDescriptor typeDescriptor = builder.create();
                    CommonUtil.registerType(typeDescriptor, gigaSpace);
                    jsonObject.addProperty(table, "Registered");
                }
            } else {
                logger.info("Number of DDLs: null");
            }
        }
        return jsonObject;
    }


    public void unregisterObject(String object) throws Exception {

        //Admin admin = commonUtil.getAdmin();
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }*/
        CommonUtil.unregisterType(object, gigaSpace);
        logger.info("end -- unregistertype");
    }
    public void quiescePU(ProcessingUnit pu){
        QuiesceRequest request = new QuiesceRequest("quiesce pu");
        pu.quiesce(request);
        boolean quiesced = pu.waitFor(QuiesceState.QUIESCED, 2, TimeUnit.MINUTES);
        if (quiesced) {
            logger.info("All instances are QUIESCED");
        }else{
            logger.info("All instances were not QUIESCED within the given timeout");
        }
    }
    public void unquiescePU(ProcessingUnit pu){
        QuiesceRequest request = new QuiesceRequest("Unquiesce pu");
        pu.unquiesce(request);
        boolean quiesced = pu.waitFor(QuiesceState.UNQUIESCED, 2, TimeUnit.MINUTES);
        if (quiesced) {
            logger.info("All instances are UN-QUIESCED");
        }else {
            logger.info("All instances were not UN-QUIESCED within the given timeout");
        }
    }
    public ProcessingUnit searchProcessingUnitByName(String puName, SearchType type){
        ProcessingUnit resultPU = null;
        logger.info("SearchProcessingUnitByName="+puName);
        Admin admin = CommonUtil.getAdmin(
                lookupLocator,
                lookupGroup,
                odsxProfile,
                gsUsername,
                gsPassword,
                appId,
                safeId,
                objectId);
        admin.getProcessingUnits().waitFor("",2, TimeUnit.SECONDS);
        logger.info("Admin api pu count="+admin.getProcessingUnits().getSize());
        logger.info("Admin api space count="+admin.getSpaces().getSpaces().length);

        for (ProcessingUnit processingUnit : admin.getProcessingUnits()) {
            switch (type){
                case EXACT:
                    if(processingUnit.getName().equals(puName)){
                        resultPU = processingUnit;
                    }
                    break;
                case PREFIX:
                    if(processingUnit.getName().startsWith(puName)){
                        resultPU = processingUnit;
                    }
                    break;
                case SUFFIX:
                    if(processingUnit.getName().endsWith(puName)){
                        resultPU = processingUnit;
                    }
                    break;
                case CONTAIN:
                    if(processingUnit.getName().contains(puName)){
                        resultPU = processingUnit;
                    }
                    break;
            }
        }
        return  resultPU;
    }
    public void undeployProcessingUnit(ProcessingUnit pu){
        logger.info("Processing Unit type = "+pu.getType());
        if(pu.getType().equals(ProcessingUnitType.STATEFUL)){
            QuiesceRequest request = new QuiesceRequest("QUIESCE PU");
            pu.quiesce(request);
            boolean quiesced = pu.waitFor(QuiesceState.QUIESCED, 1, TimeUnit.MINUTES);
            if (quiesced) {
                logger.info("All instances are QUIESCED, shutting down...");
                // wait for redo log to drop to zero
                pu.undeployAndWait(5*1000, TimeUnit.MILLISECONDS);
            }
            else {
                logger.info("All instances were not QUIESCED within the given timeout");
                // Print QuiesceDetails to figure out which instances were not QUIESCED
                logger.info("Details: " + pu.getQuiesceDetails());
                // retry or do some logic
            }
        }else{
            pu.undeployAndWait(5*1000, TimeUnit.MILLISECONDS);
        }

    }

    public void registerAndValidateInSandbox(
            String tableName, String sandboxSpace,
            String reportFilePath) throws Exception {
        logger.info("Entering into -> registerInSandbox");
        logger.info("params:  tableName -> " + tableName + " & sandboxSpace -> " + sandboxSpace);
        Admin admin = CommonUtil.getAdmin(
                lookupLocator,
                lookupGroup,
                odsxProfile,
                gsUsername,
                gsPassword,
                appId,
                safeId,
                objectId);
        //Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        //com.j_spaces.core.admin.JSpaceAdminProxy a;

        //Admin admin = (Admin) gigaSpace.getSpace().getAdmin();
        logger.info("admin: " + admin);
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        logger.info("mgr: " + mgr);
        /*GridServiceAgent gsa=  mgr.getGridServiceAgent();
        GridServiceContainer gsc1 = gsa.startGridServiceAndWait(new GridServiceContainerOptions().vmInputArgument
        ("-Dcom.gs.zones=aaa"));
        gsc1.getId();*/
        SpaceDeployment spaceDeployment = new SpaceDeployment(sandboxSpace);
        spaceDeployment.partitioned(1, 1);
        spaceDeployment.setContextProperty("pu.autogenerated-instance-sla", "true");
        if (mgr.isDeployed(sandboxSpace)) {
            mgr.undeploy(sandboxSpace);
        }
        ProcessingUnit pu = mgr.deploy(spaceDeployment);
        logger.info("mgr -> pu: " + pu);

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
        String broadcast = properties.getProperty("braodcast");
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;
        Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuildersCached = null;
        Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap = new TreeMap<>();
        Map<String, SpaceTypeDescriptor> suffixedTypeDescriptorMap = new TreeMap<>();

        result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));
        //typeDescriptorBuildersCached = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"), "_C");

        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, broadcast, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                builder = CommonUtil.setTierCriteria(tableName, builder, strTierCriteriaFile);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);

                //For validate
                baseTypeDescriptorMap.put(typeDescriptor.getTypeName(), typeDescriptor);

                logger.info("Registered object");
            }
            String ddl = CommonUtil.readDDLFromfile(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl")
                    .toString());
            typeDescriptorBuildersCached = parser.parse(ddl, TYPE_DISTINGUISHER_SUFFIX);

            for (SpaceTypeDescriptorBuilder builder : typeDescriptorBuildersCached) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, broadcast, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                //  CommonUtil.setTierCriteria(tableName + TYPE_DISTINGUISHER_SUFFIX, builder, strTierCriteriaFile);
                //CommonUtil.registerType(typeDescriptor, gigaSpace);

                //For validate
                /*registerTypesWithTieredStorageCriteria(typeDescriptorBuildersCached
                        , gigaSpace
                        , baseTypeDescriptorMap
                        , suffixedTypeDescriptorMap);*/

                builder.supportsDynamicProperties(false);
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(tableName + TYPE_DISTINGUISHER_SUFFIX)
                        .setCriteria("all"));
                SpaceTypeDescriptor typeDescriptor = builder.create();
                //  suffixedTypeDescriptorMap.put(spaceTypeDescriptor.getTypeName(), spaceTypeDescriptor);
                //  baseTypeDescriptorMap.put(typeDescriptor.getTypeName(), typeDescriptor);

                CommonUtil.registerType(typeDescriptor, gigaSpace);
                logger.info("Registered object");
            }
        } else {
            logger.info("Number of DDLs: null");
        }
        logger.info("reportData1: " + reportData);
        //3. Write records
        writeRecords(gigaSpace, baseTypeDescriptorMap);
        logger.info("reportData2: " + reportData);
        //4. Read back, compare
        readRecords(gigaSpace, baseTypeDescriptorMap);
        logger.info("reportData3: " + reportData);

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
            //return reportFilePath;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception writing report", e);
            logger.debug(" Report ");
            logger.debug(reportStr);
            logger.debug(reportAdditionalData);
        }
        logger.info("Unregistering object");
        CommonUtil.unregisterType(tableName, gigaSpace);
        logger.info("Unregistered object");
        logger.info("Removing PU");
        pu.undeploy();
        logger.info("Removed PU");
        logger.info("Exiting from -> registerInSandbox");
    }

    private void writeRecords(
            GigaSpace gigaSpace
            , Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap) {
       /* Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

        logger.info("gigaSpace: " + gigaSpace);*/

        logger.info("gigaSpace: " + gigaSpace);
        logger.info("baseTypeDescriptorMap.values(): " + baseTypeDescriptorMap.values().size());
        for (SpaceTypeDescriptor typeDescriptor : baseTypeDescriptorMap.values()) {
            String[] propertyNames = typeDescriptor.getPropertiesNames();
            SpaceDocument doc1 = new SpaceDocument(typeDescriptor.getTypeName());
            SpaceDocument doc2 = new SpaceDocument(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);

            //Reporting
            TableOutcome tableForDoc1 = new TableOutcome(typeDescriptor.getTypeName());
            TableOutcome tableForDoc2 = new TableOutcome(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);
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
            logger.info("Count in space: " + gigaSpace.count(new Object()));
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
                logger.info("Count in space after write: " + gigaSpace.count(new Object()));
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
            reportData.addTsTable(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX, tableForDoc2);
            reportData.addCachedTable(typeDescriptor.getTypeName(), tableForDoc1);

        }

    }

    private void readRecords(
            GigaSpace gigaSpace
            , Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap) {

        System.out.println("Reading back entries written just now.");
        /*Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();*/

        for (SpaceTypeDescriptor typeDescriptor : baseTypeDescriptorMap.values()) {
            // Read:

            TableOutcome tsResults = reportData.getTsResults(typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);
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

            System.out.println("Comparing records for type " + typeDescriptor.getTypeName() + " and "
                    + typeDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX);

            System.out.println("Differing records will be printed out");
            for (int i = 0; i < result1.length; i++) {
                String idPropertyName = typeDescriptor.getIdPropertiesNames().get(0);
                RecordOutcome recordOutcome1 = getRecordOutcome(csResults, result1[i].getProperty(idPropertyName));

                if (recordOutcome1 == null) {
                    logger.debug("Could not read records for id property " + result1[i].getProperty(idPropertyName));
                    String info = " Id property : " + idPropertyName;
                    info += " Value : " + result1[i].getProperty(idPropertyName);
                    csResults.additionalInfo.put(
                            "Could not retrieve record for Id value  " + info,
                            result1[i].getProperty(idPropertyName));
                } else {
                    recordOutcome1.recordRead = true;
                }

                RecordOutcome recordOutcome2 = getRecordOutcome(tsResults, result1[i].getProperty(idPropertyName));
                if (recordOutcome2 == null) {
                    //recordOutcome2.recordRead = false;
                    String info = " Id property : " + idPropertyName;
                    info += " Value : " + result1[i].getProperty(idPropertyName);
                    tsResults.additionalInfo.put(
                            "Could not retrieve record for Id value  " + info,
                            result1[i].getProperty(idPropertyName));

                } else {
                    IdQuery<SpaceDocument>
                            spaceDocumentIdQuery
                            = new IdQuery<SpaceDocument>(
                            typeDescriptor.getTypeName(),
                            result1[i].getProperty(idPropertyName));
                    SpaceDocument spaceDocument = null;
                    try {
                        spaceDocument = gigaSpace.readById(spaceDocumentIdQuery);
                    } catch (Throwable th) {
                        recordOutcome2.recordRead = false;
                        logger.debug(
                                "Exception reading  from TS for table, property " + typeDescriptor.getTypeName() + " , "
                                        + result1[i].getProperty(idPropertyName));
                        logger.error("Exception details : ", th);
                        recordOutcome2.additionalInfo.put("Exception reading record from TS ", th);
                    }
                    if (spaceDocument == null) {
                        logger.debug(" Could not find matching record for " + result1[i].getTypeName());
                        recordOutcome2.recordRead = false;

                    } else {
                        compare(result1[i], spaceDocument, recordOutcome1, recordOutcome2, gigaSpace);
                    }

                }
            }

        }
    }

    private void compare(
            SpaceDocument doc1,
            SpaceDocument doc2,
            RecordOutcome recordOutcome1,
            RecordOutcome recordOutcome2,
            GigaSpace gigaSpace) {
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

    private RecordOutcome getRecordOutcome(TableOutcome results, Object idValue) {
        for (RecordOutcome recordOutcome : results.records()) {
            if (idValue.equals(recordOutcome.getIdValue())) {
                return recordOutcome;
            }
        }
        logger.info(" Could not read recorded value  ");
        return null;
    }

    private void registerTypes(
            Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuilders,
            GigaSpace gigaSpace,
            Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap) {
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
            System.out.println(
                    "######## Successfully Register type - " + spaceTypeDescriptor.getTypeName() + " ########");
        }
    }

    private void registerTypesWithTieredStorageCriteria(
            Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuilders
            , GigaSpace gigaSpace
            , Map<String, SpaceTypeDescriptor> baseTypeDescriptorMap
            , Map<String, SpaceTypeDescriptor> suffixedTypeDescriptorMap) {
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
            for (String propName : typeDescriptorIterator.next().getPropertiesNames()) {
                logger.info("&&&&&&&&&&&&&&propName&&&&&&&&&&--------------------------" + propName);
            }

            if (typeDescriptorIterator.hasNext()) {
                SpaceTypeDescriptor tempDescriptor = typeDescriptorIterator.next();
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(tempDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX)
                        .setCriteria("all")
                );
            } else {
                System.out.println(
                        "Warning: could not find typedescriptor name - problem setting tiered storage config.");
                SpaceTypeDescriptor tempDescriptor = builder.create();
                builder.setTieredStorageTableConfig(new TieredStorageTableConfig()
                        .setName(tempDescriptor.getTypeName() + TYPE_DISTINGUISHER_SUFFIX)
                        .setCriteria("all"));
            }
            SpaceTypeDescriptor spaceTypeDescriptor = builder.create();
            suffixedTypeDescriptorMap.put(spaceTypeDescriptor.getTypeName(), spaceTypeDescriptor);
            gigaSpace.getTypeManager().registerTypeDescriptor(spaceTypeDescriptor);
            System.out.println(
                    "######## Successfully Register type - " + spaceTypeDescriptor.getTypeName() + " ########");
        }
    }

    public int objectCountInMemory(String objectType, String condition) {
        SQLQuery<SpaceDocument> query;
        if (objectType == null || objectType.equals("")) {
            query = new SQLQuery<>();
        } else if (condition == null || condition.equals("")) {
            query = new SQLQuery<>(objectType, "");
        } else {
            query = new SQLQuery<>(objectType, condition);
        }
        logger.info(">>>>>>>>>>>objectType + count " + query);
        int queryCount = gigaSpace.count(query, CountModifiers.MEMORY_ONLY_SEARCH);
        return queryCount;
    }

    public SpaceDocument objectMaxMinInMemory(String objectType, String minColName, String maxColName) {
        IJSpace space = gigaSpace.getSpace();
        space.setReadModifiers(16777216);
        SQLQuery<SpaceDocument> query;
        query = new SQLQuery<>(objectType, "min(" + minColName + "), max(" + maxColName + ")");
        SQLQuery<Object> query1;
        query1 = new SQLQuery<>(objectType, "").setProjections("max(" + minColName + ")");

        logger.info(">>>>>>>>>>>objectType + count " + query);
        //https://docs.gigaspaces.com/latest/dev-java/aggregators.html
        //        QueryExtension.max()
        AggregationResult aggregationResult = gigaSpace.aggregate(
                new SQLQuery<>(objectType, ""),
                new AggregationSet().minValue(minColName).maxValue(maxColName));
        logger.info(">>>>>>>>>>>aggregationResult + count " + aggregationResult);

        //retrieve result by index
        Integer minVal = (Integer) aggregationResult.get(0);
        Integer maxVal = (Integer) aggregationResult.get(1);
        logger.info(">>>>>>>>>>>minVal " + minVal);
        logger.info(">>>>>>>>>>>maxVal " + maxVal);
        logger.info(">>>>>>>>>>>query1 " + query1);
        Object queryCount1 = gigaSpace.read(query1, 0, ReadModifiers.MEMORY_ONLY_SEARCH);
        logger.info("queryCount1 : " + queryCount1);
        SpaceDocument queryCount = gigaSpace.read(query, 1000l, ReadModifiers.MEMORY_ONLY_SEARCH);

        return queryCount;
    }

    public String addIndex(String tableName, String propertyName, String indexType) throws ClassNotFoundException, FileNotFoundException, Exception {
        SpaceIndexType type;
        if(indexType.equals(SpaceIndexType.EQUAL.name())){
            type = SpaceIndexType.EQUAL;
        } else if(indexType.equals(SpaceIndexType.ORDERED.name())){
            type = SpaceIndexType.ORDERED;
        } else if(indexType.equals(SpaceIndexType.EQUAL_AND_ORDERED.name())){
            type = SpaceIndexType.EQUAL_AND_ORDERED;
        } else {
            logger.info("index type not passed properly "+indexType);
            return "error";
        }
        AsyncFuture<AddTypeIndexesResult> asyncAddIndex = gigaSpace.getTypeManager().asyncAddIndex(tableName,
        SpaceIndexFactory.createPropertyIndex(propertyName, type));
        logger.info("asyncAddIndex ="+asyncAddIndex.get());
        logger.info("end -- add index ");
        return "success";
    }
}
