package com.odsx.services.catalogueservice.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.response.EndpointResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import java.util.*;

@Service
public class EndpointMetadataUtils {

    private static Logger log = LoggerFactory.getLogger(EndpointMetadataUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    public EndpointResponse getMetadata(String endpointName) throws Exception {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        log.info("Endpoint Name -> " + endpointName);

        Map<String,String> serviceDefMap = consulUtils.getServiceDefinition(endpointName);

        String serviceAddress = serviceDefMap.get("ServiceAddress");
        String servicePort = serviceDefMap.get("ServicePort");
        Integer numberOfInstances = serviceDefMap.get("InstancesCount")!=null?Integer.valueOf(serviceDefMap.get("InstancesCount")):1;
        log.debug("numberOfInstances -> " + numberOfInstances);

        String endpointHost = serviceAddress+":"+servicePort;
        log.debug("Endpoint Host -> " + endpointHost);

        EndpointResponse endpointResponse = new EndpointResponse() ;
        endpointResponse.setNumberOfInstances(numberOfInstances);
        endpointResponse.setEndpointName(endpointName);
        endpointResponse.setPortNumbers(servicePort);

        String url = "http://" + endpointHost + "/v1";
        String metadataURL = url + "/"+endpointName+"/metadata";
        log.debug(" Endpoint Metadata URL -> " + metadataURL);

        endpointResponse = getMetadataFromService(metadataURL,endpointResponse);

        log.info("Exiting from ->" + methodName);

        return endpointResponse;
    }

    private List<HttpMessageConverter<?>> getTextMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new StringHttpMessageConverter());
        return converters;
    }

    private EndpointResponse getMetadataFromService(String metadataURL, EndpointResponse endpointResponse){
        String methodName = "getMetadataFromService";
        log.info("Entering into -> "+methodName);
        log.info("metadataURL -> "+metadataURL);
        RestTemplate template = new RestTemplate();
        template.setMessageConverters(getTextMessageConverters());
        try {
            String response = template.getForObject(metadataURL, String.class);
            log.info("Metadata Webservice Response -> " + response);

            List<String> metadataList = convertWSResponse(response);
            endpointResponse.setMetadata(metadataList);

            log.info("Exiting from -> "+methodName);
        } catch(Exception e){
            e.printStackTrace();
            endpointResponse.setErrorMsg("Error in connecting Metadata endpoint URL..");
        }
        return endpointResponse;
    }

    private List<String> convertWSResponse(String response){

        try {
            String methodName = "convertWSResponse";
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
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
