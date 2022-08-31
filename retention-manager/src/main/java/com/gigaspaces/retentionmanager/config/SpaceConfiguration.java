package com.gigaspaces.retentionmanager.config;

import com.gigaspaces.retentionmanager.utils.CommonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SpaceConfiguration {

    @Autowired
    private CommonUtils commonUtils;
    @Value("${space.name}")
    private String spaceName;

    @Value("${manager.info.url}")
    private String managerInfoURL;

    @Value("${manager.host}")
    private String managerHost;

    @Value("${lookup.group}")
    private String lookupGroup;

    @Bean
    public GigaSpace gigaSpace(){
        String lookupLocators =managerHost+":4174";
        return new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)
                .lookupGroups(lookupGroup)
                .lookupLocators(lookupLocators)
                ).create();
    }

    /*private String getLookupGroup(){
        String lookupGroup = "";

        RestTemplate template = new RestTemplate();
        String obj  = template.getForObject(managerInfoURL,String.class);
        String strResponse = obj.toString();
        JsonObject jsonObject = new Gson().fromJson(strResponse, JsonObject.class);
        lookupGroup = jsonObject.get("lookupGroups").getAsString();
        return lookupGroup;
    }*/

}

