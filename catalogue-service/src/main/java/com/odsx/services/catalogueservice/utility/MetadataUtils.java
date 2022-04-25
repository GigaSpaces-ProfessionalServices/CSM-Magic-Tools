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

@Component
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
            endpointName = endpointName.trim();
            try {

                log.info("Endpoint Name -> " + endpointName);
                List<Map<String,String>> serviceDefMapList = consulUtils.getServiceDefinition(endpointName);
                List<String> metadataList = new ArrayList<>();
                boolean isErrorInService = true;
                innerLoop:
                for(Map<String,String> serviceDefMap : serviceDefMapList) {

                    String serviceAddress = serviceDefMap.get("ServiceAddress");
                    String servicePort = serviceDefMap.get("ServicePort");
                    Integer numberOfInstances = serviceDefMap.get("InstancesCount") != null ? Integer.valueOf(serviceDefMap.get("InstancesCount")) : 1;
                    log.debug("numberOfInstances -> " + numberOfInstances);

                    String endpointHost = serviceAddress + ":" + servicePort;
                    log.debug("Endpoint Host -> " + endpointHost);

                    String url = "http://" + endpointHost + "/v1";
                    String metadataURL = url + "/" + endpointName + "/metadata";
                    log.debug(" Endpoint Metadata URL -> " + metadataURL);

                    try {
                        metadataList = getMetadataResponse(metadataURL);
                        log.debug(" Endpoint Metadata Response -> " + metadataList.toString());
                        isErrorInService = false;
                    }catch (Exception e){
                        servicesInError += (servicesInError == "") ? endpointName : "," + endpointName;
                        tableServiceMap.put("error",servicesInError);
                        log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
                        continue;
                    }
                    if(!isErrorInService) {
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
                        break innerLoop;
                    }
                }
                log.info("Exiting from ->" + methodName);
            }catch (Exception e ){
                log.error("Error in "+methodName+"->"+ e.getMessage());
                //e.printStackTrace();

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
            log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
            //e.printStackTrace();
            return null;
        }
    }

    private List<MetadataResponse> parseServiceMetadataRespose(Map<String,String> tableServiceMap){
        String methodName = "parseServiceMetadataRespose";
        log.info("Entering into ->" + methodName);
        List<MetadataResponse> endpointMetadataList = new ArrayList<>();
        try {
            for (String key : tableServiceMap.keySet()) {
                MetadataResponse metadataResponse = new MetadataResponse();
                String serviceNames = tableServiceMap.get(key);
                if(key==null || !key.equalsIgnoreCase("error")) {
                    metadataResponse.setTableName(key.trim());
                } else{
                    metadataResponse.setErrorMsg("Services in error");
                }
                metadataResponse.setServiceList(Arrays.asList(serviceNames.split(",")));
                endpointMetadataList.add(metadataResponse);
            }
        }catch (Exception e){
            log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
        }
        return endpointMetadataList;
    }

}
