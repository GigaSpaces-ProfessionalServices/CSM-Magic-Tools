package com.odsx.services.catalogueservice.controller;

import com.odsx.services.catalogueservice.utility.ConsulUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CrossOrigin
@Controller
public class EndpointController {

    private static Logger log = LoggerFactory.getLogger(EndpointController.class);

    @Resource
    private ConsulUtils consulUtils;

    @GetMapping("/")
    public ModelAndView loadEndpoints(){

        log.info("Entering into -> loadEndpoints");
        ModelAndView modelAndView = new ModelAndView();
        try{

            List<String> endpointList = consulUtils.getAllRegisteredEndpoints();

            log.info("Endpoint List Size -> "+(endpointList!=null ? endpointList.size() : "is null"));
            modelAndView.addObject("endpointList", endpointList);
        } catch (Exception e){
            log.error(e.getLocalizedMessage());
            modelAndView.addObject("error", "Error in retrieving endpoints from consul. ");
        }
        modelAndView.setViewName("endpoints");

        log.info("Exiting from -> loadEndpoints");
        return modelAndView;
    }

}
