package com.gigaspaces.retentionmanager.service;

import com.gigaspaces.retentionmanager.RunRetentionManager;
import com.gigaspaces.retentionmanager.utils.CommonUtils;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {
    private static final Logger log = LoggerFactory.getLogger(RunRetentionManager.class);
    @Autowired
    private InfluxDB influxDB;

    @Autowired
    private CommonUtils commonUtils;

    @Value("${influxdb.database}")
    private String database;


    public boolean verifyConnection(){
        log.info("Entering into -> verifyConnection");
        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            log.error("Error pinging server.");
            return false;
        } else{
            log.info("InfluxDB connected successfully!");
            log.info("Exiting from -> verifyConnection");
            return true;

        }
    }

    public void connectToDatabase(){
        if(influxDB.databaseExists(database)){
            influxDB.setDatabase(database);
        } else{
            influxDB.createDatabase(database);
            influxDB.setDatabase(database);
        }
    }

    public void writeMeasurement(String objectType,Integer deletedRecords, Date cleanupStarted){
        log.info("Entering in to -> writeMeasurement");
        log.debug("cleanupStarted->"+cleanupStarted);
        try {
            connectToDatabase();
            long executionTime = commonUtils.findDateDifference(cleanupStarted, new Date(), "");
            Point point = Point.measurement("object_cleanup")
                    .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .tag("object", objectType)
                    .addField("deletedRecords", deletedRecords)
                    .addField("executionTime", executionTime)
                    .build();

            influxDB.write(point);
            log.info("Exiting from -> writeMeasurement");
        } catch (Exception e){
            e.printStackTrace();
            log.error("Error in writeMeasurement -> "+e.getLocalizedMessage(),e);
        }
    }

}
