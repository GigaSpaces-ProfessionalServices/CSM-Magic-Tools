package com.odsx.services.catalogueservice.utility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.beans.EndpointMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.ConnectException;
import java.util.*;

@Service
public class EndpointMetadataUtils {

    private static Logger log = LoggerFactory.getLogger(EndpointMetadataUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    public EndpointMetadata getMetadata(String endpointName) throws Exception {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        EndpointMetadata endpointMetadata = null ;
        //try {

            Map<String,String> serviceDefMap = consulUtils.getServiceDefinition(endpointName);

            String serviceAddress = serviceDefMap.get("ServiceAddress");
            String servicePort = serviceDefMap.get("ServicePort");
            Integer numberOfInstances = serviceDefMap.get("InstancesCount")!=null?Integer.valueOf(serviceDefMap.get("InstancesCount")):1;
            String endpointHost = serviceAddress+":"+servicePort;
            log.debug("Endpoint Host -> " + endpointHost);


            String url = "http://" + endpointHost + "/v1";
            log.debug("Endpoint Name -> " + endpointName);
            log.debug("Endpoint URL -> " + url);

            Map<String, String> params = new HashMap<String, String>();
            params.put("serviceName", endpointName.toLowerCase(Locale.ROOT));

            String restURL = url + "/"+endpointName+"/metadata";
            log.debug("Metadata URL -> " + restURL);

            RestTemplate template = new RestTemplate();
            template.setMessageConverters(getTextMessageConverters());

            String response = template.getForObject(restURL, String.class);
            log.info("Metadata Webservice Response -> " + response);

            endpointMetadata = convertWSResponse(response);
            endpointMetadata.setNumberOfInstances(numberOfInstances);

            log.info("Exiting from ->" + methodName);

        /*} catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());

        }*/
        return endpointMetadata;
    }

    private List<HttpMessageConverter<?>> getTextMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new StringHttpMessageConverter());
        return converters;
    }

    private EndpointMetadata convertWSResponse(String response){

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

            EndpointMetadata endpointMetadata = new EndpointMetadata();
            endpointMetadata.setMetadata(metadataList);
            endpointMetadata.setEndpointName(endpointName);

            log.info("Exiting from ->" + methodName);
            return endpointMetadata;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
