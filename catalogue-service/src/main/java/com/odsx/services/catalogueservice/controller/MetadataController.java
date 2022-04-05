package com.odsx.services.catalogueservice.controller;

import com.odsx.services.catalogueservice.utility.ConsulUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import java.util.*;

@CrossOrigin
@RestController
public class MetadataController {

    private static Logger log = LoggerFactory.getLogger(MetadataController.class);

    @Resource
    private ConsulUtils consulUtils;

    @GetMapping(value = "/tables/{endpointName}")
    public String getMetadata(@PathVariable String endpointName) {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        try {
            List<String> endpointList = consulUtils.getAllRegisteredEndpoints();

            log.debug("Total Endpoints -> "+endpointList.size());
            if (endpointList.isEmpty()) {
                return null;
            }
            //String endpointName = endpointList.get(Integer.valueOf(endpointIndex));
            String endpointHost = consulUtils.getServiceDefinition(endpointName);

            String url = "http://" + endpointHost + "/v1";
            log.debug("Endpoint Name -> " + endpointName);
            log.debug("Endpoint URL -> " + url);

            Map<String, String> params = new HashMap<String, String>();
            params.put("serviceName", endpointName.toLowerCase(Locale.ROOT));
            //String restURL = url + "/{serviceName}/metadata";
            String restURL = url + "/"+endpointName+"/metadata";
            log.debug("Metadata URL -> " + restURL);

            RestTemplate template = new RestTemplate();
            template.setMessageConverters(getTextMessageConverters());

            String response = template.getForObject(restURL, String.class);
            log.info("Returned Metadata  -> " + response);

            log.info("Exiting from ->" + methodName);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            return null;
        }
    }

    private List<HttpMessageConverter<?>> getTextMessageConverters() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new StringHttpMessageConverter());
        return converters;
    }
}
