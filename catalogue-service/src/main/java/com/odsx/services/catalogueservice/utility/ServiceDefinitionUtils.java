package com.odsx.services.catalogueservice.utility;

import com.odsx.services.catalogueservice.model.ServiceDefinition;
import com.odsx.services.catalogueservice.model.InstanceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ServiceDefinitionUtils {

    private static Logger log = LoggerFactory.getLogger(ConsulUtils.class);
    public String buildMetadataURL(InstanceDefinition instanceDefinition){
        String methodName = "buildMetadataURL";
        log.info("Entering into -> "+methodName);
        log.debug("InstanceDefinition ->"+instanceDefinition);
        String url = "";
        if(instanceDefinition.getHostName()!=null && instanceDefinition.getHostName()!=""
                && instanceDefinition.getServiceName()!=null && instanceDefinition.getServiceName()!=""
                && instanceDefinition.getPortNumber()!=null && instanceDefinition.getPortNumber()!="") {
            url = "http://" + instanceDefinition.getHostName() + ":" + instanceDefinition.getPortNumber() + "/v1/metadata";

        }
        log.debug("Metadata URL  -> "+url);
        log.info("Exiting from -> "+methodName);
        return url;
    }

}
