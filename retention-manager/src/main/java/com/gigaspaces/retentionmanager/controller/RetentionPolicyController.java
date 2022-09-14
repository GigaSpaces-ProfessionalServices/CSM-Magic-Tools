package com.gigaspaces.retentionmanager.controller;

import com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy;
import com.gigaspaces.retentionmanager.service.ObjectRetentionPolicyService;
import com.gigaspaces.retentionmanager.service.RetentionManagerService;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.google.gson.*;
import javax.annotation.Resource;
import java.util.*;
import java.util.logging.Logger;

@RestController
public class RetentionPolicyController {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RetentionPolicyController.class);
    @Resource
    private ObjectRetentionPolicyService objectRetentionPolicyService;

    private Logger logger = Logger.getLogger(this.getClass().getName());


    @GetMapping("/retention/policies")
    public  Map<String, String> getAllRetentionPolicies() {
        Map<String,String> responseMap = new HashMap<>();
        List<ObjectRetentionPolicy> allRetentionPolicies = objectRetentionPolicyService.getAllRetentionPolicies();
        Gson gson = new Gson();
        String jsonList = gson.toJson(allRetentionPolicies);
        responseMap.put("response",jsonList);
        return responseMap;
    }

    @GetMapping("/retention/policies/{objectType}")
    public  Map<String, String> getRetentionPolicy(@PathVariable String objectType) {
        Map<String,String> responseMap = new HashMap<>();
        List<ObjectRetentionPolicy> objectRetentionPolicyList = objectRetentionPolicyService.getRetentionPolicy(objectType);
        Gson gson = new Gson();
        String jsonObj = gson.toJson(objectRetentionPolicyList);
        responseMap.put("response",jsonObj);
        return responseMap;
    }

    @PostMapping(path = "/retention/policies", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> addRetentionPolicy(@RequestBody ObjectRetentionPolicy objectRetentionPolicy) {
        Map<String,String> responseMap = new HashMap<>();
        objectRetentionPolicyService.addRetentionPolicy(objectRetentionPolicy);
        responseMap.put("response","Success: Retention Policy for "+objectRetentionPolicy.getObjectType()+" is registered successfully.");
        return responseMap;
    }

    @PutMapping(path = "/retention/policies/{id}")
    public Map<String,String> updateRetentionPolicy(@PathVariable String objectType) {
        Map<String,String> responseMap = new HashMap<>();
        List<ObjectRetentionPolicy> list = objectRetentionPolicyService.getRetentionPolicy(objectType);
        for(ObjectRetentionPolicy objectRetentionPolicy:list) {
            objectRetentionPolicyService.updateRetentionPolicy(objectRetentionPolicy);
        }
            responseMap.put("response","Success: Retention Policy for "+objectType+" is updated successfully.");
            return responseMap;
    }

    @PutMapping(path = "/retention/policies", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> updateRetentionPolicy(@RequestBody ObjectRetentionPolicy objectRetentionPolicy) {
        Map<String,String> responseMap = new HashMap<>();
        System.out.println("objectRetentionPolicy--->"+objectRetentionPolicy.getActive());
        objectRetentionPolicyService.updateRetentionPolicy(objectRetentionPolicy);
        responseMap.put("response","Success: Retention Policy for "+objectRetentionPolicy.getObjectType()+" is updated successfully.");
        return responseMap;
    }

    @DeleteMapping(path = "/retention/policies/{objectType}")
    public Map<String,String> deleteRetentionPolicy(@PathVariable String objectType) {
        Map<String,String> responseMap = new HashMap<>();
        List<ObjectRetentionPolicy> list = objectRetentionPolicyService.getRetentionPolicy(objectType);
        for(ObjectRetentionPolicy objectRetentionPolicy:list) {
            objectRetentionPolicyService.deleteRetentionPolicy(objectRetentionPolicy.getObjectType());
        }

        responseMap.put("response","Success: Retention Policy for "+objectType+" is removed successfully.");
        return responseMap;
    }
}
