package com.gigaspaces.datavalidator.utils;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InfluxDBUtils {

    private static Logger logger = Logger.getLogger(InfluxDBUtils.class.getName());

    private static InfluxDB influxDB;

    public static void doConnect(String databaseURL, String userName,String password){
        influxDB = InfluxDBFactory.connect(databaseURL, userName, password);
    }
    public static void write(String database, String measurement, String tabEnv, String tagObjType, String state, String host){
        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            logger.info("Error pinging InfluxDB server.");
        }else{
            logger.info("InfluxDB connected successfully");
        }
        influxDB.createDatabase(database);
        int result = 0; // 0=fail , 1=pass
        if(state != null && state.equals("PASS")){
            result=1;
        }
        Point point = Point.measurement(measurement)
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("env", tabEnv)
                .tag("obj_type",tagObjType)
                .tag("host",host)
                .addField("state",state)
                .addField("result",result)
                .build();

        influxDB.write(database,"",point);
        influxDB.close();
    }
}
