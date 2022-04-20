package com.odsx.services.catalogueservice.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.response.MetadataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MetadataUtils {
    private static Logger log = LoggerFactory.getLogger(MetadataUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    public List<MetadataResponse> getService(List<String> allEndpoints) throws Exception {
        String methodName = "getService";
        log.info("Entering into ->" + methodName);
        Map<String,String> tableServiceMap = new HashMap<>();
        String servicesInError="";
        for(String endpointName: allEndpoints) {
            try {
                log.info("Endpoint Name -> " + endpointName);
                Map<String, String> serviceDefMap = consulUtils.getServiceDefinition(endpointName);

                String serviceAddress = serviceDefMap.get("ServiceAddress");
                String servicePort = serviceDefMap.get("ServicePort");
                Integer numberOfInstances = serviceDefMap.get("InstancesCount") != null ? Integer.valueOf(serviceDefMap.get("InstancesCount")) : 1;
                log.debug("numberOfInstances -> " + numberOfInstances);

                String endpointHost = serviceAddress + ":" + servicePort;
                log.debug("Endpoint Host -> " + endpointHost);

                String url = "http://" + endpointHost + "/v1";
                String metadataURL = url + "/" + endpointName + "/metadata";
                log.debug(" Endpoint Metadata URL -> " + metadataURL);

                List<String> metadataList = getMetadataResponse(metadataURL);
                log.debug(" Endpoint Metadata Response -> " + metadataList.toString());

                for (String tableName : metadataList) {
                    String mapValue = "";
                    if (tableServiceMap.containsKey(tableName)) {
                        mapValue = tableServiceMap.get(tableName);

                        if (!mapValue.contains(endpointName))
                            mapValue += (mapValue == "") ? mapValue : "," + endpointName;

                        tableServiceMap.put(tableName, mapValue);
                    } else {
                        tableServiceMap.put(tableName, endpointName);
                    }
                }

                log.info("Exiting from ->" + methodName);
            }catch (Exception exception ){
                log.error("Error in "+methodName+"->"+ exception.getMessage());
                exception.printStackTrace();
                servicesInError += (servicesInError == "") ? endpointName : "," + endpointName;
                tableServiceMap.put("ErrorInService",servicesInError);
            }
        }
        log.debug(" Table Service Map -> " + tableServiceMap.toString());
        return parseServiceMetadataRespose(tableServiceMap);
    }

    private List<String> getMetadataResponse(String metadataURL) throws Exception{
        String methodName = "getMetadataResponse";
        log.info("Entering into -> "+methodName);
        log.info("metadataURL -> "+metadataURL);
        RestTemplate template = new RestTemplate();

        String response = template.getForObject(metadataURL, String.class);
        log.info("Metadata Webservice Response -> " + response);

        List<String> metadataList = convertMetadataWSResponse(response);

        log.info("Exiting from -> "+methodName);

        return metadataList;
    }

    private List<String> convertMetadataWSResponse(String response){
        String methodName = "convertMetadataWSResponse";
        log.info("Entering into ->" + methodName);
        try {
            log.debug("Response -> " + response);
            List<String> metadataList = new ArrayList<>();

            JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);

            String endpointName = jsonObject.get("endpoint").getAsString();
            log.info("Endpoint Name -> " + endpointName);
            JsonArray metadataArray = jsonObject.getAsJsonArray("metadata").getAsJsonArray();
            log.info("Metadata Array -> " + metadataArray.toString());

            for (JsonElement jsonElement : metadataArray) {
                metadataList.add(jsonElement.getAsString());
            }
            log.info("Exiting from ->" + methodName);
            return metadataList;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private List<MetadataResponse> parseServiceMetadataRespose(Map<String,String> tableServiceMap){
        List<MetadataResponse> endpointMetadataList = new ArrayList<>();
        for (String key : tableServiceMap.keySet()) {
            String serviceNames = tableServiceMap.get(key);
            MetadataResponse metadataResponse = new MetadataResponse();
            metadataResponse.setTableName(key);
            metadataResponse.setServiceList(Arrays.asList(serviceNames.split(",")));
            endpointMetadataList.add(metadataResponse);
        }
        return endpointMetadataList;
    }

}
