package com.odsx.services.catalogueservice.controller;

import com.odsx.services.catalogueservice.response.MetadataResponse;
import com.odsx.services.catalogueservice.utility.ConsulUtils;
import com.odsx.services.catalogueservice.utility.EndpointUtils;
import com.odsx.services.catalogueservice.utility.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@Controller
public class MetadataController {

    private static Logger log = LoggerFactory.getLogger(MetadataController.class);

    @Resource
    private MetadataUtils metadataUtils;

    @Resource
    private ConsulUtils consulUtils;

  /*  @GetMapping("/metadata")
    public ModelAndView loadMetadata(){

        log.info("Entering into -> loadEndpoints");
        ModelAndView modelAndView = new ModelAndView();
        List<EndpointResponse> endpointMetadataList = new ArrayList<>();
        try{

            List<String> endpointList = consulUtils.getAllRegisteredEndpoints();

            log.info("Endpoint List Size -> "+(endpointList!=null ? endpointList.size() : "is null"));

            for(String endpoint : endpointList){

                EndpointResponse endpointResponse = new EndpointResponse();
                endpointResponse.setEndpointName(endpoint);
                endpointMetadataList.add(endpointResponse);
            }
            modelAndView.addObject("endpointMetadataList", endpointMetadataList);
        } catch (Exception e){
            log.error(e.getLocalizedMessage());
            modelAndView.addObject("error", "Error in retrieving endpoints from consul. ");
        }
        modelAndView.setViewName("metadata");

        log.info("Exiting from -> loadEndpoints");
        return modelAndView;
    }*/

    @GetMapping("/metadata")
    public ModelAndView getServicesForTable(){

        log.info("Entering into -> getServicesForTable");
        ModelAndView modelAndView = new ModelAndView();
        List<MetadataResponse> endpointMetadataList = new ArrayList<>();

        try {
            List<String> endpointList = consulUtils.getAllRegisteredEndpoints();
            log.info(" All endpoints -> "+endpointList.toString());
            endpointMetadataList = metadataUtils.getService(endpointList);
        } catch (Exception e){
            e.printStackTrace();
            modelAndView.addObject("error", "Error in retrieving tables and its services. ");
        }


        log.debug("Endpoint with metadata Size -> "+(endpointMetadataList!=null ? endpointMetadataList.size() : "is null"));
        log.info("Exiting from -> getServicesForTable");
        modelAndView.addObject("endpointMetadataList", endpointMetadataList);

        modelAndView.setViewName("metadata");

        return modelAndView;
    }

}
