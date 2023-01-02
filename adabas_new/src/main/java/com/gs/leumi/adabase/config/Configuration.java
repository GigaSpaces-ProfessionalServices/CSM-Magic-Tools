package com.gs.leumi.adabase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties("config")
public class Configuration {

    private Zookeeper zookeeper;
    private Kafka kafka;
    private String schemasPath;

    public Zookeeper getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(Zookeeper zookeeper) {

        this.zookeeper = zookeeper;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public String getSchemasPath() {
        return schemasPath;
    }

    public void setSchemasPath(String schemasPath) {
        this.schemasPath = schemasPath;
    }
}
