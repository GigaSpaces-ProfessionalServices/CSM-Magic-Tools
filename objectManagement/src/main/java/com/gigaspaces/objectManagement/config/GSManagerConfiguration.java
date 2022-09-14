package com.gigaspaces.objectManagement.config;

import com.gigaspaces.objectManagement.controller.ObjectController;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Logger;

@Configuration
public class GSManagerConfiguration {
//    private Logger logger = Logger.getLogger(this.getClass().getName());
    private org.slf4j.Logger logger = LoggerFactory.getLogger(GSManagerConfiguration.class);

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

    @Value("${safe.appid}")
    private String APP_ID;

    @Value("${safe.safeid}")
    private String SAFE_ID;

    @Value("${safe.objectid}")
    private String OBJECT_ID;


    //@Bean
    /**
     * Commented bean creation of Admin because most of the operations can be done by space proxy object ,
     * as we have all details to get space proxy at application level
    */
    private  void setCredentials() throws IOException, InterruptedException {
        String managerHost = LOOKUP_GROUP.split(",")[0].split(":")[0];
        logger.info("LOOKUP_GROUP : "+LOOKUP_GROUP);
        logger.info("managerHost : "+managerHost);

        Process p = Runtime.getRuntime().exec("ssh "+managerHost);
        PrintStream out = new PrintStream(p.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String safeusernameCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID="+APP_ID+" -p Query=\"Safe="+SAFE_ID+";Folder=;Object="+OBJECT_ID+";\" -o PassProps.UserName";
        String safepassCmd = "/opt/CARKaim/sdk/clipasswordsdk GetPassword -p AppDescs.AppID="+APP_ID+" -p Query=\"Safe="+SAFE_ID+";Folder=;Object="+OBJECT_ID+";\" -o Password";
        logger.info("safeusernameCmd :: "+safeusernameCmd);
        logger.info("safepassCmd :: "+safepassCmd);
        out.println(safeusernameCmd);
        while (in.ready()) {
            GS_USERNAME = in.readLine();
            logger.info(GS_USERNAME);
            break;
        }
        out.println(safepassCmd);
        while (in.ready()) {
            GS_PASSWORD = in.readLine();
            logger.info(GS_PASSWORD);
            break;
        }
        out.println("exit");
        p.waitFor();
    }
    public Admin getGSAdmin(){
        logger.info("Generating bean -> getGSAdmin:Admin");
        AdminFactory adminFactory = new AdminFactory();
        adminFactory.addLocators(LOOKUP_LOCATOR);

        if(LOOKUP_GROUP!=null && LOOKUP_GROUP!=""){
            adminFactory.addGroups(LOOKUP_GROUP);
        }
        if (PROFILE!=null && PROFILE!="" && PROFILE.equalsIgnoreCase("security")) {
            try {
                setCredentials();
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(),e);
            } catch (InterruptedException e) {
                logger.error(e.getLocalizedMessage(),e);
            }
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

