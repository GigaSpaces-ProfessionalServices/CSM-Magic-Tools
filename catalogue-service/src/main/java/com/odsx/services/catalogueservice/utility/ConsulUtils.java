package com.odsx.services.catalogueservice.utility;

import com.google.gson.*;
import com.odsx.services.catalogueservice.model.InstanceDefinition;
import com.odsx.services.catalogueservice.model.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class ConsulUtils {

    private static Logger log = LoggerFactory.getLogger(ConsulUtils.class);

    @Value("${consul.catalog.endpoint}")
    private String CONSUL_CATALOG_ENDPOINT;

    @Resource
    private CommonUtils commonUtils;

    public List<String> getAllRegisteredEndpoints() throws Exception{
        String methodName = "getAllRegisteredEndpoints";
        log.info("Entering into -> "+methodName);

        List<String> catalogServiceList = new ArrayList<>();

        String CONSUL_URL = getCatalogServicesEndpoint();

        log.debug("CONSUL URL -> "+CONSUL_URL);
        String strResponse = commonUtils.getStringRestResponse(CONSUL_URL);

        log.info("Consul Catalog Services Response -> "+strResponse);
        JsonObject convertedObject = new Gson().fromJson(strResponse, JsonObject.class);

        for (Map.Entry entry : convertedObject.entrySet()) {
            if (!entry.getKey().toString().equalsIgnoreCase("consul")) {
                catalogServiceList.add((String) entry.getKey());
            }
        }
        log.info("Total Endpoints registered in Consul -> "+catalogServiceList.size());

        Collections.sort(catalogServiceList);
        log.info("Exiting from -> "+methodName);
        return catalogServiceList;
    }

    public ServiceDefinition getServiceDefinition(String serviceName) throws Exception{
        String methodName = "getServiceDefinition";
        log.info("Entering into -> "+methodName);

        log.info("Service name -> "+serviceName);
        String CONSUL_URL = getServiceDefEndpoint(serviceName);
        log.debug("Consul Service Definition URL -> "+CONSUL_URL);

        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setName(serviceName);
        String strResponse = commonUtils.getStringRestResponse(CONSUL_URL);

        log.debug("Consul Service Definition Response -> "+strResponse);

        JsonArray jsonArray = new Gson().fromJson(strResponse, JsonArray.class);
        List<InstanceDefinition> instanceDefList = new ArrayList<>();
        for(JsonElement jsonElement : jsonArray) {
            InstanceDefinition instanceDefinition = new InstanceDefinition();
            instanceDefinition.setServiceName(serviceName);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            log.debug("Service Address -> " + jsonObject.get("ServiceAddress"));
            log.debug("Service Port -> " + jsonObject.get("ServicePort"));
            log.debug("Number of Instances -> " + String.valueOf(jsonArray.size()));

            instanceDefinition.setHostName(jsonObject.get("ServiceAddress").getAsString());
            instanceDefinition.setPortNumber(jsonObject.get("ServicePort").getAsString());


            log.debug("InstanceDefinition -> " + instanceDefinition);
            instanceDefList.add(instanceDefinition);
            serviceDefinition.setInstances(instanceDefList);

        }
        log.debug("SeviceDefinition -> " + serviceDefinition);
        log.info("Exiting from -> " + methodName);
        return serviceDefinition;
    }

    private String getCatalogServicesEndpoint(){
        return  CONSUL_CATALOG_ENDPOINT + "/services";
    }

    private String getServiceDefEndpoint(String serviceName){
        return CONSUL_CATALOG_ENDPOINT+"/service/"+(serviceName!=null?serviceName.trim():"");
    }

}
