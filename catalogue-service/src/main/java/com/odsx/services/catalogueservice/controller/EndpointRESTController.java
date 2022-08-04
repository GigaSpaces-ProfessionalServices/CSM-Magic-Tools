package com.odsx.services.catalogueservice.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.odsx.services.catalogueservice.model.ServiceDefinition;
import com.odsx.services.catalogueservice.utility.ConsulUtils;
import com.odsx.services.catalogueservice.utility.EndpointUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

@RestController
@CrossOrigin
public class EndpointRESTController {
    @Resource
    private EndpointUtils endpointUtils;

    @Resource
    private ConsulUtils consulUtils;

    @GetMapping("/service/{serviceName}/try")
    public String checkServiceHealth(@PathVariable String serviceName) {

        boolean success = endpointUtils.tryService(serviceName);
        if(success){
            return "success";
        }
        return "error";
    }
}

