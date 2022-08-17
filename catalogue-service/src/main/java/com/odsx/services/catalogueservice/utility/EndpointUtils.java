package com.odsx.services.catalogueservice.utility;

import com.google.gson.*;
import com.odsx.services.catalogueservice.model.InstanceDefinition;
import com.odsx.services.catalogueservice.model.ServiceDefinition;
import com.odsx.services.catalogueservice.response.EndpointResponse;
import com.odsx.services.catalogueservice.response.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

@Service
public class EndpointUtils {

    private static Logger log = LoggerFactory.getLogger(EndpointUtils.class);

    @Resource
    private ConsulUtils consulUtils;

    @Resource
    private CommonUtils commonUtils;

    @Resource
    private InstanceDefinitionUtils instanceDefinitionUtils;

    public List<String> getAllEndpoints() throws Exception {
        List<String> endpointList = consulUtils.getAllRegisteredEndpoints();
        return endpointList;
    }

    public List<EndpointResponse> getMetadata(String[] endpointArr) {
        String methodName = "getMetadata";
        log.info("Entering into ->" + methodName);
        log.info("Endpoint Name -> " + endpointArr.toString());
        List<EndpointResponse> endpointResponseList = new ArrayList<>();

        for(String endpointName : endpointArr) {
            endpointName = endpointName.trim();
            int totalInstances = 0;
            int healthyInstances = 0;
            InstanceDefinition tempInstanceDef = null;
            try {
                ServiceDefinition serviceDefinition = consulUtils.getServiceDefinition(endpointName);
                totalInstances=serviceDefinition.getInstances().size();

                for(InstanceDefinition instanceDefinition : serviceDefinition.getInstances()) {
                    log.debug("InstanceDefinition-> " + instanceDefinition);
                    boolean isHealthy = instanceDefinitionUtils.isHealthyInstance(instanceDefinition);
                    if(isHealthy){
                        healthyInstances+=1;
                        tempInstanceDef = instanceDefinition;
                    }
                }

                EndpointResponse endpointResponse = new EndpointResponse();
                endpointResponse.setEndpointName(endpointName);
                endpointResponse.setNumberOfInstances(serviceDefinition.getInstances().size());
                endpointResponse.setHealthStatus(getServiceHealthStatus(healthyInstances,totalInstances));
                if(tempInstanceDef!=null) {
                    endpointResponse.setPortNumbers(tempInstanceDef.getPortNumber());
                    String metadataURL = instanceDefinitionUtils.buildMetadataURL(tempInstanceDef);
                    log.debug("Endpoint Metadata URL -> " + metadataURL);

                    endpointResponse = getEndpointResponse(metadataURL, endpointResponse);

                } else{
                    endpointResponse.setErrorMsg("Error in connecting service endpoint");
                }

                endpointResponseList.add(endpointResponse);
                log.info("Exiting from ->" + methodName);
            } catch (Exception e) {
                log.error("Error in "+methodName+" -> "+e.getLocalizedMessage());
                //e.printStackTrace();
            }
        }
        return endpointResponseList;
    }

    private EndpointResponse getEndpointResponse(String metadataURL, EndpointResponse endpointResponse) {
        String methodName = "getEndpointResponse";
        log.info("Entering into -> "+methodName);
        log.info("metadataURL -> "+metadataURL);
        try {
            String response = commonUtils.getStringRestResponse(metadataURL);
            JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
            log.info("Metadata Webservice Response -> " + response);
            List<String> metadataList = commonUtils.getJsonArrayValue(jsonObject, "metadata");
            String project = commonUtils.getJsonStringValue(jsonObject, "project");
            String description = commonUtils.getJsonStringValue(jsonObject, "description");
            endpointResponse.setMetadata(metadataList);
            endpointResponse.setProject(project);
            endpointResponse.setDescription(description);
            log.info("Exiting from -> " + methodName);
        } catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            endpointResponse.setErrorMsg("Error in getting response from metadata endpoint.");
        }
        return endpointResponse;
    }


    private HealthStatus getServiceHealthStatus(int healthyInstances, int totalInstances) {
        log.info("Entering into -> getServiceHealthStatus");
        double healthPercentage = 0.0;
        try {
            healthPercentage = healthyInstances * 100 / totalInstances;
            log.debug("Health Percentage of Service -> "+healthPercentage);
        } catch (Exception e){
            log.error("Error in calculating service health status "+e.getMessage());
        }
        if(healthPercentage == 100.0){
            log.info("Exiting -> getServiceHealthStatus");
            return HealthStatus.HEALTHY;
        } else if(healthPercentage == 0.0){
            log.info("Exiting -> getServiceHealthStatus");
            return HealthStatus.UNHEALTHY;
        } else{
            log.info("Exiting -> getServiceHealthStatus");
            return HealthStatus.DEGRADED;
        }
    }

    public Boolean tryService(String serviceName){
        log.info("Entering into -> tryService ");
        try {
            log.debug("serviceName-> "+serviceName);
            ServiceDefinition serviceDefinition = consulUtils.getServiceDefinition(serviceName);
            for(InstanceDefinition instanceDefinition : serviceDefinition.getInstances()) {
                log.debug("Instance Definition -> "+instanceDefinition);
                boolean isHealthy = instanceDefinitionUtils.isHealthyInstance(instanceDefinition);
                if(isHealthy){
                    String metadataURL = instanceDefinitionUtils.buildMetadataURL(instanceDefinition);
                    String command = "curl -I "+metadataURL;
                    log.debug("curl command to execute -> "+command);
                    Process process = Runtime.getRuntime().exec(command);
                    process.waitFor();
                    process.destroy();
                    log.info("process.exitValue -> "+process.exitValue());
                    if(process.exitValue()==0){
                        log.info("Exiting from tryService ");
                        return true;
                    }
                }
            }

        }catch (Exception e){
            log.error("Exception in tryService "+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}
