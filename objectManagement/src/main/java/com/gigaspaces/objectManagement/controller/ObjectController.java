package com.gigaspaces.objectManagement.controller;

import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.metadata.PropertyInfo;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.objectManagement.model.SpaceObjectDto;
import com.gigaspaces.objectManagement.service.DdlParser;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import java.util.concurrent.TimeUnit;

@RestController
public class ObjectController {


    private GigaSpace gigaSpace;
    @Autowired
    private DdlParser parser;

    @GetMapping("/list")
    public JsonArray getObjectList(@RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator) {
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject;

        //admin.getGridServiceAgents().waitForAtLeastOne();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            jsonObject = new JsonObject();
            System.out.println("Space name:" + space.getUid());
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
                    System.out.println(entry.getKey() + " " + entry.getValue());
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
                        System.out.println(pi.getName());
                        System.out.println(pi.getType());
                        spaceObjectDto.add(spaceObject);
                        jsonArray2.add(jsonObject2);
                    }
                    jsonObject3.add("columns", jsonArray2);
                    jsonArray3.add(jsonObject3);
                }
                jsonObject.add("objects", jsonArray3);
                for (String className : strings) {
                    if (className.equals("java.lang.Object")) continue;
                    System.out.println("ClassName: " + className);
                }
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    @PostMapping("/registertype/batch")
    public String registerTypeBatch(@RequestParam("tableListfilePath") String tableListfilePath, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("spaceName") String spaceName, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator) throws ClassNotFoundException, FileNotFoundException {
        System.out.println("tableListfilePath" + tableListfilePath);
        System.out.println("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        System.out.println("gigaSpace: " + gigaSpace);
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
                System.out.println("Number of DDLs: " + result.size());

                for (SpaceTypeDescriptorBuilder builder : result) {

                    CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                    CommonUtil.addRouting(routing, builder);
                    CommonUtil.addIndex(index, indexType, builder);
                    CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                    SpaceTypeDescriptor typeDescriptor = builder.create();
                    CommonUtil.registerType(typeDescriptor, gigaSpace);
                }
            } else {
                System.out.println("Number of DDLs: null");
            }
        }
        return "Registered";
    }

    @PostMapping("/unregistertype")
    public String unregisterType(@RequestParam("spaceName") String spaceName, @RequestParam("type") String type, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator) throws ClassNotFoundException, FileNotFoundException {
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        System.out.println("gigaSpace: " + gigaSpace);
        CommonUtil.unregisterType(type, gigaSpace);
        return "Unregistered";
    }


    @PostMapping("/registertype/single")
    public String registerTypeSingle(@RequestParam("tableName") String tableName, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("spaceName") String spaceName, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator) throws ClassNotFoundException, FileNotFoundException {
        System.out.println("tableName" + tableName);
        System.out.println("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        admin.getSpaces().waitFor("", 1, TimeUnit.SECONDS);
        for (Space space : admin.getSpaces()) {
            if (spaceName.equals(space.getName())) {
                gigaSpace = space.getGigaSpace();
                break;
            }
        }
        System.out.println("gigaSpace: " + gigaSpace);

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
            System.out.println("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
            }
        } else {
            System.out.println("Number of DDLs: null");
        }
        return "Registered";
    }


    @PostMapping("/registertype/sandbox")
    public String registerTypeSandbox(@RequestParam("tableName") String tableName, @RequestParam("ddlAndPropertiesBasePath") String ddlAndPropertiesBasePath, @RequestParam("lookupGroup") String lookupGroup, @RequestParam("lookupLocator") String lookupLocator) throws ClassNotFoundException, FileNotFoundException {
        String spaceName = "sandboxSpace";
        System.out.println("tableName" + tableName);
        System.out.println("ddlAndPropertiesBasePath" + ddlAndPropertiesBasePath);
        if (lookupLocator == null || "".equals(lookupLocator)) {
            lookupLocator = "localhost";
        }
        if (lookupGroup == null || "".equals(lookupGroup)) {
            lookupGroup = "xap-16.2.0";
        }
        Admin admin = new AdminFactory().addLocator(lookupLocator).addGroups(lookupGroup).createAdmin();
        GridServiceManager mgr = admin.getGridServiceManagers()
                .waitForAtLeastOne();
        ProcessingUnit pu = mgr.deploy(new SpaceDeployment(spaceName)
                .partitioned(1, 1));

        GigaSpace gigaSpace = pu.waitForSpace().getGigaSpace();

        System.out.println("gigaSpace: " + gigaSpace);

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
            System.out.println("Number of DDLs: " + result.size());

            for (SpaceTypeDescriptorBuilder builder : result) {

                CommonUtil.addSpaceId(spaceId, spaceIdType, builder);
                CommonUtil.addRouting(routing, builder);
                CommonUtil.addIndex(index, indexType, builder);
                CommonUtil.dynamicPropertiesSupport(Boolean.getBoolean(supportDynamicProperties), builder);
                SpaceTypeDescriptor typeDescriptor = builder.create();
                CommonUtil.registerType(typeDescriptor, gigaSpace);
                System.out.println("Registered object");
            }
        } else {
            System.out.println("Number of DDLs: null");
        }
        System.out.println("Unregistering object");
        CommonUtil.unregisterType(tableName, gigaSpace);
        System.out.println("Unregistered object");
        System.out.println("Removing PU");
        pu.undeploy();
        System.out.println("Removed PU");
        return "Done";
    }

}
