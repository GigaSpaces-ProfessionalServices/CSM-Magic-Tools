package com.odsx.services.catalogueservice.utility;

import com.odsx.services.catalogueservice.model.InstanceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class InstanceDefinitionUtils {

    private static Logger log = LoggerFactory.getLogger(InstanceDefinitionUtils.class);
    @Resource
    private CommonUtils commonUtils;
    public String buildMetadataURL(InstanceDefinition instanceDefinition){
        String methodName = "buildMetadataURL";
        log.info("Entering into -> "+methodName);
        log.debug("InstanceDefinition ->"+instanceDefinition);
        String url = "";
        if(instanceDefinition.getHostName()!=null && instanceDefinition.getHostName()!=""
                && instanceDefinition.getServiceName()!=null && instanceDefinition.getServiceName()!=""
                && instanceDefinition.getPortNumber()!=null && instanceDefinition.getPortNumber()!="") {
            url = "http://" + instanceDefinition.getHostName() + ":" + instanceDefinition.getPortNumber() + "/v1/metadata";
            //url = "http://" + instanceDefinition.getHostName() + ":" + instanceDefinition.getPortNumber() + "/v1/"+instanceDefinition.getServiceName()+"/metadata";

        }
        log.debug("Metadata URL  -> "+url);
        log.info("Exiting from -> "+methodName);
        return url;
    }

    public String buildHealthURL(InstanceDefinition instanceDefinition){
        String methodName = "buildHealthURL";
        log.info("Entering into -> "+methodName);
        log.debug("InstanceDefinition ->"+instanceDefinition);
        String url = "";
        if(instanceDefinition.getHostName()!=null && instanceDefinition.getHostName()!=""
                && instanceDefinition.getServiceName()!=null && instanceDefinition.getServiceName()!=""
                && instanceDefinition.getPortNumber()!=null && instanceDefinition.getPortNumber()!="") {
            url = "http://" + instanceDefinition.getHostName() + ":" + instanceDefinition.getPortNumber() + "/v1/actuator/health";
        }
        log.debug("Metadata URL  -> "+url);
        log.info("Exiting from -> "+methodName);
        return url;
    }

    public Boolean isHealthyInstance(InstanceDefinition instanceDefinition){
        String methodName = "isHealthyInstance";
        log.info("Entering into -> "+methodName);
        boolean isHealthy = false;
        try {
            String instanceHealthURL = buildHealthURL(instanceDefinition);
            String response = commonUtils.getStringRestResponse(instanceHealthURL);
            log.debug("Health Response -> "+response);
            if (response.equalsIgnoreCase("\"PASSED\"")) {
                isHealthy = true;
            }
            log.info("Exiting from -> " + methodName);
        }catch (Exception e){
            log.error("Exception in connecting health URL"+e.getMessage());
        }
        return isHealthy;
    }
}
