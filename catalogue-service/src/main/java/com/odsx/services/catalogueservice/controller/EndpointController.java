package com.odsx.services.catalogueservice.controller;

import com.odsx.services.catalogueservice.beans.EndpointMetadata;
import com.odsx.services.catalogueservice.utility.ConsulUtils;
import com.odsx.services.catalogueservice.utility.EndpointMetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@Controller
public class EndpointController {

    private static Logger log = LoggerFactory.getLogger(EndpointController.class);

    @Resource
    private EndpointMetadataUtils endpointMetadataUtils;

    @Resource
    private ConsulUtils consulUtils;

    @GetMapping("/")
    public ModelAndView loadEndpoints(){

        log.info("Entering into -> loadEndpoints");
        ModelAndView modelAndView = new ModelAndView();
        List<EndpointMetadata> endpointMetadataList = new ArrayList<>();
        try{

            List<String> endpointList = consulUtils.getAllRegisteredEndpoints();

            log.info("Endpoint List Size -> "+(endpointList!=null ? endpointList.size() : "is null"));

            for(String endpoint : endpointList){

                EndpointMetadata endpointMetadata = new EndpointMetadata();
                endpointMetadata.setEndpointName(endpoint);
                endpointMetadataList.add(endpointMetadata);
            }
            modelAndView.addObject("endpointList", endpointList);
            modelAndView.addObject("endpointMetadataList", endpointMetadataList);
        } catch (Exception e){
            log.error(e.getLocalizedMessage());
            modelAndView.addObject("error", "Error in retrieving endpoints from consul. ");
        }
        modelAndView.setViewName("endpoints");

        log.info("Exiting from -> loadEndpoints");
        return modelAndView;
    }

    @PostMapping("/")
    public ModelAndView getEndpointMetadata(@RequestParam(value = "allEndpoints") String endpoints){

        log.info("Entering into -> getEndpointMetadata");
        ModelAndView modelAndView = new ModelAndView();
        List<EndpointMetadata> endpointMetadataList = new ArrayList<>();
        log.info("Request Params - All endpoints -> "+endpoints);



        endpoints = endpoints.substring(1,endpoints.length()-1);

        String[] endpointList = endpoints.split(",");
        log.debug("Endpoints As Array ->"+endpointList.toString());

        for(String endpoint : endpointList){
            EndpointMetadata endpointMetadata = null;
            try {
                 endpointMetadata = endpointMetadataUtils.getMetadata(endpoint);

                log.debug("endpointMetadata -> " + endpointMetadata.toString());
                endpointMetadataList.add(endpointMetadata);
            } catch (Exception e){

                endpointMetadata = new EndpointMetadata();
                endpointMetadata.setEndpointName(endpoint);
                endpointMetadata.setErrorMsg("Metatada endpoint is not accessible.");
                endpointMetadataList.add(endpointMetadata);

            }
        }

        log.info("Endpoint with metadata Size -> "+(endpointMetadataList!=null ? endpointMetadataList.size() : "is null"));
        log.info("Exiting from -> getEndpointMetadata");
        modelAndView.addObject("endpointMetadataList", endpointMetadataList);

        modelAndView.setViewName("endpoints");


        return modelAndView;
    }

}
