package com.odsx.services.catalogueservice.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.model.InstanceDefinition;
import com.odsx.services.catalogueservice.model.ServiceDefinition;
import com.odsx.services.catalogueservice.response.MetadataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MetadataUtils {
    private static Logger log = LoggerFactory.getLogger(MetadataUtils.class);

    @Resource
    private ConsulUtils consulUtils;
    @Resource
    private InstanceDefinitionUtils instanceDefinitionUtils;

    @Resource
    private CommonUtils commonUtils;

    public List<MetadataResponse> getService(List<String> allEndpoints) throws Exception {
        String methodName = "getService";
        log.info("Entering into ->" + methodName);
        Map<String,String> tableServiceMap = new HashMap<>();
        String servicesInError="";
        for(String endpointName: allEndpoints) {
            endpointName = endpointName.trim();
            try {

                log.info("Endpoint Name -> " + endpointName);
                ServiceDefinition serviceDefinition = consulUtils.getServiceDefinition(endpointName);
                List<String> metadataList = new ArrayList<>();
                boolean isErrorInService = true;
                innerLoop:
                for(InstanceDefinition instanceDefinition : serviceDefinition.getInstances()) {

                    log.debug("InstanceDefinition -> " + instanceDefinition.toString());
                    String metadataURL = instanceDefinitionUtils.buildMetadataURL(instanceDefinition);
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

        String response = commonUtils.getStringRestResponse(metadataURL);
        log.info("Metadata Webservice Response -> " + response);
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        List<String> metadataList = commonUtils.getJsonArrayValue(jsonObject,"metadata");

        log.info("Exiting from -> "+methodName);

        return metadataList;
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
