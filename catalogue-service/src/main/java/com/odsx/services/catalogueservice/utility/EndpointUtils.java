package com.odsx.services.catalogueservice.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.response.EndpointResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import java.util.*;

@Service
public class EndpointUtils {

    private static Logger log = LoggerFactory.getLogger(EndpointUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    public List<String> getAllEndpoints() throws Exception {
        List<String> endpointList = consulUtils.getAllRegisteredEndpoints();
        return endpointList;
    }

    public List<EndpointResponse> getMetadata(String[] endpointArr) {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        log.info("Endpoint Name -> " + endpointArr.toString());
        List<EndpointResponse> endpointResponseList = new ArrayList<>();
        for(String endpointName : endpointArr) {
            EndpointResponse endpointResponse = new EndpointResponse();
            endpointResponse.setEndpointName(endpointName);
            try {
                Map<String, String> serviceDefMap = consulUtils.getServiceDefinition(endpointName);

                String serviceAddress = serviceDefMap.get("ServiceAddress");
                String servicePort = serviceDefMap.get("ServicePort");
                Integer numberOfInstances = serviceDefMap.get("InstancesCount") != null ? Integer.valueOf(serviceDefMap.get("InstancesCount")) : 1;
                log.debug("numberOfInstances -> " + numberOfInstances);

                String endpointHost = serviceAddress + ":" + servicePort;
                log.debug("Endpoint Host -> " + endpointHost);

                endpointResponse.setNumberOfInstances(numberOfInstances);

                endpointResponse.setPortNumbers(servicePort);

                String url = "http://" + endpointHost + "/v1";
                String metadataURL = url + "/" + endpointName + "/metadata";
                log.debug(" Endpoint Metadata URL -> " + metadataURL);

                List<String> metadataList = getEndpointResponse(metadataURL);
                endpointResponse.setMetadata(metadataList);
            } catch (Exception e) {
                e.printStackTrace();
                endpointResponse.setErrorMsg("Error in connecting Metadata endpoint URL..");
            }
            log.info("Exiting from ->" + methodName);
            endpointResponseList.add(endpointResponse);

        }
        return endpointResponseList;
    }

    private List<String> getEndpointResponse(String metadataURL) throws Exception{
        String methodName = "getEndpointResponse";
        log.info("Entering into -> "+methodName);
        log.info("metadataURL -> "+metadataURL);
        RestTemplate template = new RestTemplate();
        String response = template.getForObject(metadataURL, String.class);
        log.info("Metadata Webservice Response -> " + response);
        List<String> metadataList = convertEndpointWSResponse(response);
        log.info("Exiting from -> "+methodName);

        return metadataList;
    }

    private List<String> convertEndpointWSResponse(String response) {
        try {
            String methodName = "convertEndpointWSResponse";
            log.info("Entering into ->" + methodName);

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
