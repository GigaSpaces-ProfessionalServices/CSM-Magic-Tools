package com.gigaspaces.objectManagement.service;

import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.metadata.PropertyInfo;
import com.gigaspaces.metadata.SpacePropertyDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.objectManagement.model.SpaceObjectDto;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.admin.IRemoteJSpaceAdmin;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ObjectService {
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

    public void registerObject(String tableName) throws ClassNotFoundException, FileNotFoundException, Exception {
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
        }
        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                builder = CommonUtil.setTierCriteria(tableName,builder,strTierCriteriaFile);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
            }
        } else {
            logger.info("Number of DDLs: null");
        }
        logger.info("end -- registertype single");
    }

    public JsonArray listObjects() throws FileNotFoundException, Exception{
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;

        IRemoteJSpaceAdmin remoteAdmin =
                (IRemoteJSpaceAdmin)gigaSpace.getSpace().getAdmin();

        Object classList[] = remoteAdmin.getRuntimeInfo().m_ClassNames.toArray();
        List<SpaceObjectDto> spaceObjectDto = new ArrayList<>();
        GigaSpaceTypeManager gigaSpaceTypeManager = gigaSpace.getTypeManager();
        //((IRemoteJSpaceAdmin) gigaSpace.getSpace().getAdmin()).getName()
        jsonObject = new JsonObject();
        jsonObject.addProperty("spacename", gigaSpace.getSpaceName());
        JsonObject jsonObject3;
        JsonArray jsonArray3 = new JsonArray();
        for(Object obj : classList){
            String objectType = obj.toString().trim();
            if (objectType.equals("java.lang.Object")) continue;
            JsonArray jsonArray2 = new JsonArray();
            jsonObject3 = new JsonObject();
            jsonObject3.addProperty("tablename", objectType);
            String objPropFile = objectType;
            if(objectType.endsWith("_C")) {
                objPropFile = objectType.substring(0, objectType.lastIndexOf("_"));
            }
            Properties properties = CommonUtil.readProperties(ddlAndPropertiesBasePath + objPropFile);
            String spaceId = properties.getProperty("spaceId") != null ? properties.getProperty("spaceId") : "";
            String routing = properties.getProperty("routing") != null ? properties.getProperty("routing") : "";
            String index = properties.getProperty("index") != null ? properties.getProperty("index") : "";

            String criteria = CommonUtil.getTierCriteriaConfig(objectType,strTierCriteriaFile);

            jsonObject3.addProperty("spaceId", spaceId);
            jsonObject3.addProperty("routing", routing);
            jsonObject3.addProperty("index", index);
            jsonObject3.addProperty("criteria", criteria!=null && criteria.trim()!=""?criteria:"");


            logger.info("####################");
            SpaceTypeDescriptor spaceTypeDescriptor = gigaSpaceTypeManager.getTypeDescriptor(objectType);
            String[] objPropsArr = spaceTypeDescriptor.getPropertiesNames();
            String spaceIdProp = spaceTypeDescriptor.getIdPropertyName();
            logger.info("spaceIdProp =>"+spaceIdProp);
            List<String> idPropsList = spaceTypeDescriptor.getIdPropertiesNames();
            for (int i = 0; i < objPropsArr.length; i++) {
                String prop = objPropsArr[i];
                SpacePropertyDescriptor propertyDescriptor = spaceTypeDescriptor.getFixedProperty(prop);
                //spaceTypeDescriptor
                System.out.println("  Name:" + propertyDescriptor.getName() + " Type:" + propertyDescriptor.getTypeName() + " Storage Type:"
                        + propertyDescriptor.getStorageType());
                JsonObject jsonObject2 = new JsonObject();
                SpaceObjectDto spaceObject = new SpaceObjectDto();

                jsonObject2.addProperty("columnname", propertyDescriptor.getName());
                jsonObject2.addProperty("columntype", propertyDescriptor.getTypeName());
                jsonObject2.addProperty("isSpacePrimitive", idPropsList.toString());

                spaceObject.setObjName(propertyDescriptor.getName());
                spaceObject.setObjtype(String.valueOf(propertyDescriptor.getTypeName()));
                spaceObject.setSpaceId(idPropsList.toString());
                spaceObject.setSpaceName(spaceName);
                logger.info(propertyDescriptor.getName() + " -> " + propertyDescriptor.getTypeName());
                spaceObjectDto.add(spaceObject);
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

    public void registerObjectBatch() throws FileNotFoundException, Exception{
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
                logger.info("Number of DDLs: " + result.size());

                for (SpaceTypeDescriptorBuilder builder : result) {

                    CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                    CommonUtil.addRouting(routing, builder);
                    CommonUtil.addIndex(index, indexType, builder);
                    //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                    builder = CommonUtil.setTierCriteria(table,builder,strTierCriteriaFile);
                    SpaceTypeDescriptor typeDescriptor = builder.create();
                    CommonUtil.registerType(typeDescriptor, gigaSpace);
                }
            } else {
                logger.info("Number of DDLs: null");
            }
        }
    }

    public void unregisterObject(String object) throws  Exception{

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

    public void registerInSandbox(String tableName, String sandboxSpace) throws  Exception{
        logger.info("Entering into -> registerInSandbox");
        logger.info("params:  tableName -> "+tableName+" & sandboxSpace -> "+sandboxSpace   );
        Admin admin = CommonUtil.getAdmin(lookupLocator,lookupGroup,odsxProfile,gsUsername,gsPassword);
        //Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(sandboxSpace)
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


        result = parser.parse(Paths.get(ddlAndPropertiesBasePath + tableName + ".ddl"));

        if (result != null) {
            logger.info("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                //CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                builder = CommonUtil.setTierCriteria(tableName,builder,strTierCriteriaFile);
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
