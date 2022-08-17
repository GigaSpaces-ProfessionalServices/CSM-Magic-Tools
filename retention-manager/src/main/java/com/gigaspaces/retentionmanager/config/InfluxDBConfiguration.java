package com.gigaspaces.retentionmanager.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfiguration {

    @Value("${influxdb.url}")
    private String databaseURL;

    @Value("${influxdb.user}")
    private String userName;

    @Value("${influxdb.password}")
    private String password;

    @Bean
    public InfluxDB connect(){
        InfluxDB influxDB = InfluxDBFactory.connect(databaseURL, userName, password);
        return influxDB;
    }
}
