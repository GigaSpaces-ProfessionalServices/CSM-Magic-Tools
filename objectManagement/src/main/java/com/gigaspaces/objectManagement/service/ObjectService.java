package com.gigaspaces.objectManagement.service;

import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.objectManagement.model.SpaceObjectDto;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.admin.IRemoteJSpaceAdmin;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class ObjectService {
    @Value("${safe.appid}")
    String appId;
    @Value("${safe.safeid}")
    String safeId;
    @Value("${safe.objectid}")
    String objectId;
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
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;

        try {
            result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));

        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
        if (result != null) {
            logger.info("gigaSpace.getTypeManager().getTypeDescriptor(tableName) : " + gigaSpace.getTypeManager().getTypeDescriptor(tableName));
            if (gigaSpace.getTypeManager().getTypeDescriptor(tableName) != null) {
                return "duplicate";
            }
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
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

        Object classList[] = remoteAdmin.getRuntimeInfo().m_ClassNames.toArray();
        List<SpaceObjectDto> spaceObjectDto = new ArrayList<>();
        GigaSpaceTypeManager gigaSpaceTypeManager = gigaSpace.getTypeManager();
        //((IRemoteJSpaceAdmin) gigaSpace.getSpace().getAdmin()).getName()
        jsonObject = new JsonObject();
        jsonObject.addProperty("spacename", gigaSpace.getSpaceName());
        JsonObject jsonObject3;
        JsonArray jsonArray3 = new JsonArray();
        for (Object obj : classList) {
            String objectType = obj.toString().trim();
            if (objectType.equals("java.lang.Object")) continue;
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
            String criteria = CommonUtil.getTierCriteriaConfig(objectType, strTierCriteriaFile);

            jsonObject3.addProperty("spaceId", spaceId);
            jsonObject3.addProperty("routing", routing);
            jsonObject3.addProperty("index", index);
            jsonObject3.addProperty("criteria", criteria != null && criteria.trim() != "" ? criteria : "");


            logger.info("####################");
            SpaceTypeDescriptor spaceTypeDescriptor = gigaSpaceTypeManager.getTypeDescriptor(objectType);
            String[] propertiesName = spaceTypeDescriptor.getPropertiesNames();
            String[] propertiesType = spaceTypeDescriptor.getPropertiesTypes();

            List<String> spaceIdProp = spaceTypeDescriptor.getIdPropertiesNames();
            logger.info("spaceIdProp =>" + spaceIdProp);
            List<String> idPropsList = spaceTypeDescriptor.getIdPropertiesNames();
            //List<String> spaceIdList = spaceTypeDescriptor.getIdPropertiesNames();
            String routingPropertyName = spaceTypeDescriptor.getRoutingPropertyName();
            Map<String, SpaceIndex> indexesMap = spaceTypeDescriptor.getIndexes();
            String criteriaName = "";
            String criteriaFieldname = "";
            logger.info(">>>>spaceTypeDescriptor.getTieredStorageTableConfig() :" + spaceTypeDescriptor.getTieredStorageTableConfig());
            if (spaceTypeDescriptor.getTieredStorageTableConfig() != null) {
                criteriaName = spaceTypeDescriptor.getTieredStorageTableConfig().getCriteria();
                criteriaFieldname = criteriaName;
                if (criteriaName.split("<").length > 1) {
                    criteriaFieldname = criteriaName.split("<")[0];
                } else if (criteriaName.split(">").length > 1) {
                    criteriaFieldname = criteriaName.split(">")[0];
                } else if (criteriaName.split("=").length > 1) {
                    criteriaFieldname = criteriaName.split("=")[0];
                }
            }
            for (int i = 0; i < propertiesName.length; i++) {
                String prop = propertiesName[i];
                //SpacePropertyDescriptor propertyDescriptor = spaceTypeDescriptor.getFixedProperty(prop);
                //spaceTypeDescriptor
                //System.out.println("  Name:" + propertyDescriptor.getName() + " Type:" + propertyDescriptor.getTypeName() + " Storage Type:"
                //       + propertyDescriptor.getStorageType());
                JsonObject jsonObject2 = new JsonObject();
                //     SpaceObjectDto spaceObject = new SpaceObjectDto();

                jsonObject2.addProperty("columnname", propertiesName[i]);
                jsonObject2.addProperty("columntype", propertiesType[i]);

                jsonObject2.addProperty("spaceId", idPropsList.contains(propertiesName[i]) ? "Yes" : "");
                jsonObject2.addProperty("spaceRouting", propertiesName[i].equals(routingPropertyName) ? "Yes" : "");
                jsonObject2.addProperty("spaceIndex", indexesMap.containsKey(propertiesName[i]) ? indexesMap.get(propertiesName[i]).getIndexType().name() : "");
                jsonObject2.addProperty("tierCriteria", propertiesName[i].equals(criteriaFieldname) ? criteriaName : "");
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
            // supportDynamicProperties = properties.getProperty("supportDynamicProperties");

            // DdlParser parser = new DdlParser();
            Collection<SpaceTypeDescriptorBuilder> result = null;

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

                    CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                    CommonUtil.addRouting(routing, builder);
                    CommonUtil.addIndex(index, indexType, builder);
                    //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
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
        logger.info("gigaSpace: " + gigaSpace);
        CommonUtil.unregisterType(object, gigaSpace);
        logger.info("end -- unregistertype");
    }

    public void registerInSandbox(String tableName, String sandboxSpace) throws Exception {
        logger.info("Entering into -> registerInSandbox");
        logger.info("params:  tableName -> " + tableName + " & sandboxSpace -> " + sandboxSpace);
        Admin admin = CommonUtil.getAdmin(lookupLocator, lookupGroup, odsxProfile, gsUsername, gsPassword, appId, safeId, objectId);
        //Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        //com.j_spaces.core.admin.JSpaceAdminProxy a;

        //Admin admin = (Admin) gigaSpace.getSpace().getAdmin();
        logger.info("admin: " + admin);
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        logger.info("mgr: " + mgr);
        SpaceDeployment spaceDeployment = new SpaceDeployment(sandboxSpace);
        spaceDeployment.partitioned(1, 1);
        spaceDeployment.setContextProperty("pu.autogenerated-instance-sla", "true");

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
        String supportDynamicProperties = properties.getProperty("supportDynamicProperties");

        // DdlParser parser = new DdlParser();
        Collection<SpaceTypeDescriptorBuilder> result = null;
        Collection<SpaceTypeDescriptorBuilder> typeDescriptorBuildersCached = null;

        result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));
        typeDescriptorBuildersCached = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"), "_C");

        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                builder = CommonUtil.setTierCriteria(tableName, builder, strTierCriteriaFile);
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
        logger.info("Exiting from -> registerInSandbox");
    }
}
