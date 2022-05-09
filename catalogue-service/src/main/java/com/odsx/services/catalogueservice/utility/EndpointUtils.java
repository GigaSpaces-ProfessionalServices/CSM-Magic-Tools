package com.odsx.services.catalogueservice.utility;

import com.google.gson.*;
import com.odsx.services.catalogueservice.model.InstanceDefinition;
import com.odsx.services.catalogueservice.model.ServiceDefinition;
import com.odsx.services.catalogueservice.response.EndpointResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

@Service
public class EndpointUtils {

    private static Logger log = LoggerFactory.getLogger(EndpointUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    @Resource
    private CommonUtils commonUtils;

    @Resource
    private ServiceDefinitionUtils serviceDefinitionUtils;

    public List<String> getAllEndpoints() throws Exception {
        List<String> endpointList = consulUtils.getAllRegisteredEndpoints();
        return endpointList;
    }

    public List<EndpointResponse> getMetadata(String[] endpointArr) {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        log.info("Endpoint Name -> " + endpointArr.toString());
        List<EndpointResponse> endpointResponseList = new ArrayList<>();
        int count =0;
        for(String endpointName : endpointArr) {
            endpointName = endpointName.trim();
            count++;
            try {
                ServiceDefinition serviceDefinition = consulUtils.getServiceDefinition(endpointName);

                List<String> metadataList = new ArrayList<>();
                EndpointResponse endpointResponse = new EndpointResponse();
                endpointResponse.setEndpointName(endpointName);
                boolean isServiceInError = true;

                innerLoop:
                for(InstanceDefinition instanceDefinition : serviceDefinition.getInstances()) {


                    log.debug("InstanceDefinition-> " + instanceDefinition);

                    endpointResponse.setNumberOfInstances(serviceDefinition.getInstances().size());

                    endpointResponse.setPortNumbers(instanceDefinition.getPortNumber());

                    String metadataURL = serviceDefinitionUtils.buildMetadataURL(instanceDefinition);
                    log.debug(" Endpoint Metadata URL -> " + metadataURL);

                    try {
                        metadataList = getEndpointResponse(metadataURL);
                        isServiceInError = false;
                        break innerLoop;
                    } catch (Exception e){
                        log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
                        continue;
                    }
                }
                if(!isServiceInError) {
                    endpointResponse.setMetadata(metadataList);
                } else{
                    endpointResponse.setErrorMsg("Error in connecting Metadata endpoint URL.");
                }
                endpointResponseList.add(endpointResponse);
                log.info("Exiting from ->" + methodName);
            } catch (Exception e) {
                log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
                //e.printStackTrace();
            }
        }
        return endpointResponseList;
    }

    private List<String> getEndpointResponse(String metadataURL) throws Exception{
        String methodName = "getEndpointResponse";
        log.info("Entering into -> "+methodName);
        log.info("metadataURL -> "+metadataURL);
        List<String> metadataList = new ArrayList<>();

        String response = commonUtils.getStringRestResponse(metadataURL);
        log.info("Metadata Webservice Response -> " + response);
        metadataList = convertEndpointWSResponse(response);
        log.info("Exiting from -> " + methodName);

        return metadataList;
    }

    private List<String> convertEndpointWSResponse(String response) {
        String methodName = "convertEndpointWSResponse";
        try {

            log.info("Entering into ->" + methodName);

            log.debug("Response -> " + response);
            List<String> metadataList = new ArrayList<>();

            JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);

            //jsonObject.
            String endpointName = jsonObject.get("endpoint").getAsString();
            log.info("Endpoint Name -> " + endpointName);
            JsonArray metadataArray = jsonObject.getAsJsonArray("metadata").getAsJsonArray();
            log.info("Metadata Array -> " + metadataArray.toString());

            for (JsonElement jsonElement : metadataArray) {
                metadataList.add(jsonElement.getAsString());
            }
            log.info("Exiting from ->" + methodName);
            return metadataList;
        } catch (Exception e) {
            log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
            e.printStackTrace();
            return null;
        }
    }


}
