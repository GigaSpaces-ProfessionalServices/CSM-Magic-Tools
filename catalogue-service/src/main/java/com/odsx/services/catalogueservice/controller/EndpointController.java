package com.odsx.services.catalogueservice.controller;

import com.odsx.services.catalogueservice.response.EndpointResponse;
import com.odsx.services.catalogueservice.utility.EndpointUtils;
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
    private EndpointUtils endpointUtils;

    @GetMapping("/")
    public ModelAndView loadEndpoints(){

        log.info("Entering into -> loadEndpoints");
        ModelAndView modelAndView = new ModelAndView();
        List<EndpointResponse> endpointMetadataList = new ArrayList<>();
        try{

            List<String> endpointList = endpointUtils.getAllEndpoints();

            log.info("Endpoint List Size -> "+(endpointList!=null ? endpointList.size() : "is null"));

            for(String endpoint : endpointList){

                EndpointResponse endpointResponse = new EndpointResponse();
                endpointResponse.setEndpointName(endpoint);
                endpointMetadataList.add(endpointResponse);
            }
            modelAndView.addObject("endpointList", endpointList);
            modelAndView.addObject("endpointResponseList", endpointMetadataList);
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
        log.info("Request Params - All endpoints -> "+endpoints);
        ModelAndView modelAndView = new ModelAndView();
        try{

            List<EndpointResponse> endpointResponseList = new ArrayList<>();

            endpoints = endpoints.substring(1,endpoints.length()-1);

            String[] endpointList = endpoints.split(",");
            log.debug("Endpoints As Array ->"+endpointList.toString());

            endpointResponseList = endpointUtils.getMetadata(endpointList);
            Integer totalInstances = endpointResponseList.stream().map(x -> x.getNumberOfInstances())
                        .reduce(0, Integer::sum);

            modelAndView.addObject("totalInstances", totalInstances);
            modelAndView.addObject("endpointResponseList", endpointResponseList);
            modelAndView.setViewName("endpoints");

            log.info("Endpoint with metadata Size -> "+(endpointResponseList!=null ? endpointResponseList.size() : "is null"));
            log.info("Exiting from -> getEndpointMetadata");
        } catch (Exception e){
            log.error(e.getLocalizedMessage());
            modelAndView.addObject("error", "Error in retrieving endpoints from consul. ");
        }

        return modelAndView;
    }

}
