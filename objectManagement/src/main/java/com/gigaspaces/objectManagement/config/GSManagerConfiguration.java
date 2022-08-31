package com.gigaspaces.objectManagement.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.SpaceDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Logger;

@Configuration
public class GSManagerConfiguration {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Value("${odsx.profile}")
    private String PROFILE;

    @Value("${gs.username}")
    private String GS_USERNAME;

    @Value("${gs.password}")
    private String GS_PASSWORD;

    @Value("${lookup.group}")
    private String LOOKUP_GROUP;

    @Value("${lookup.locator}")
    private String LOOKUP_LOCATOR;

    @Value("${space.name}")
    private String SPACE_NAME;

    //@Bean
    /**
     * Commented bean creation of Admin because most of the operations can be done by space proxy object ,
     * as we have all details to get space proxy at application level
    */
    public Admin getGSAdmin(){
        logger.info("Generating bean -> getGSAdmin:Admin");
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.addLocator(LOOKUP_LOCATOR);

        if(LOOKUP_GROUP!=null && LOOKUP_GROUP!=""){
            adminFactory.addGroups(LOOKUP_GROUP);
        }
        if (PROFILE!=null && PROFILE!="" && PROFILE.equalsIgnoreCase("security")) {
            adminFactory.credentials(GS_USERNAME, GS_PASSWORD);
        }
        Admin admin =adminFactory.createAdmin();
        logger.info("Bean Admin -> "+admin.toString());
        return admin;
    }

    @Bean
    public GigaSpace gigaSpace(){

        logger.info("gigaSpace() -------------------> "+PROFILE);
        SpaceProxyConfigurer configurer = new SpaceProxyConfigurer(SPACE_NAME);
        if(LOOKUP_GROUP!=null && LOOKUP_GROUP!=""){
            configurer.lookupGroups(LOOKUP_GROUP);
        }
        /*
        * No need to pass security credentials to create a space proxy object because gs manager is secured but space is not secured.
        * if (PROFILE!=null && PROFILE!="" && PROFILE.equalsIgnoreCase("security")) {
            logger.info("setting credentials");
            configurer.credentials(GS_USERNAME, GS_PASSWORD);
        }*/
        configurer.lookupLocators(LOOKUP_LOCATOR);
        return new GigaSpaceConfigurer(configurer).create();
    }

}

