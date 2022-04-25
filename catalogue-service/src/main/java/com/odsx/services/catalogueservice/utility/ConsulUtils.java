package com.odsx.services.catalogueservice.utility;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class ConsulUtils {

    private static Logger log = LoggerFactory.getLogger(ConsulUtils.class);

    @Value("${consul.catalog.endpoint}")
    private String CONSUL_CATALOG_ENDPOINT;

    public List<String> getAllRegisteredEndpoints() throws Exception{
        String methodName = "getAllRegisteredEndpoints";
        log.info("Entering into -> "+methodName);

        List<String> catalogServiceList = new ArrayList<>();

        String CONSUL_URL = CONSUL_CATALOG_ENDPOINT + "/services";

        log.debug("CONSUL URL -> "+CONSUL_URL);
        RestTemplate template = getRestTemplate();
        Object obj = template.getForObject(CONSUL_URL, Object.class);
        String strResponse = obj.toString();

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

    public List<Map<String,String>> getServiceDefinition(String serviceName) throws Exception{
        String methodName = "getServiceDefinition";
        log.info("Entering into -> "+methodName);

        log.info("Service name -> "+serviceName);
        List<Map<String,String>> instancesMapList = new ArrayList<>();


        String CONSUL_URL = CONSUL_CATALOG_ENDPOINT+"/service/"+(serviceName!=null?serviceName.trim():"");
        log.debug("Consul Service Definition URL -> "+CONSUL_URL);
        RestTemplate template = getRestTemplate();
        String obj  = template.getForObject(CONSUL_URL,String.class);
        String strResponse = obj.toString();

        log.debug("Consul Service Definition Response -> "+strResponse);

        JsonArray jsonArray = new Gson().fromJson(strResponse, JsonArray.class);
        for(JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            log.debug("Service Address -> " + jsonObject.get("ServiceAddress"));
            log.debug("Service Port -> " + jsonObject.get("ServicePort"));
            log.debug("Number of Instances -> " + String.valueOf(jsonArray.size()));
            Map<String, String> serviceDefMap = new HashMap<>();
            serviceDefMap.put("ServiceAddress", jsonObject.get("ServiceAddress").getAsString());
            serviceDefMap.put("ServicePort", jsonObject.get("ServicePort").getAsString());

            log.info("Exiting from -> " + methodName);
            instancesMapList.add(serviceDefMap);
        }
        return instancesMapList;
    }

    private RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
