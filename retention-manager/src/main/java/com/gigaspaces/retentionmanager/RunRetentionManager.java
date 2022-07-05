package com.gigaspaces.retentionmanager;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.gigaspaces.retentionmanager.service.InfluxDBService;
import com.gigaspaces.retentionmanager.service.RetentionManagerService;
import jdk.nashorn.internal.ir.IdentNode;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RunRetentionManager {

    private static final Logger log = LoggerFactory.getLogger(RunRetentionManager.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Value("${scheduler.config}")
    private String schedulerConfig;

    @Autowired
    private RetentionManagerService retentionManagerService;

    @Scheduled(fixedDelayString = "${scheduler.interval}")
    public void cleanUpSpaceObjects() {
        log.info("cleanUpSpaceObjects");
        if(schedulerConfig.equalsIgnoreCase("interval")){
            runCleanup();
        } else{
            log.info("Skipped cleanUpSpaceObjects because scheduler is running at specific time");
        }
    }

    @Scheduled(cron = "${cron.expression}")
    public void cleanUpSpaceObjectsAtSpecificTime() {
        log.info("cleanUpSpaceObjectsAtSpecificTime");
        if(schedulerConfig.equalsIgnoreCase("timely")){
            runCleanup();
        } else{
            log.info("Skipped cleanUpSpaceObjectsAtSpecificTime because scheduler is running at regular interval");
        }
    }

    private void runCleanup(){
        String methodName= "runCleanup";
        log.info("Entering in to -> "+methodName);
        log.info("Cleanup process started at "+new Date());
        retentionManagerService.cleanUpSpaceData();
        log.info("Cleanup process completed at "+new Date());
        log.info("Exiting from -> "+methodName);
    }

}