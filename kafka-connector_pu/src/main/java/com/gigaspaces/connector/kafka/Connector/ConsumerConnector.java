package com.gigaspaces.connector.kafka.Connector;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.data.DataHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
//@Profile(Consts.CONNECTOR_MODE)
public class ConsumerConnector {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerConnector.class);

    @Autowired
    private DataHandler dataHandler;

    @Autowired
    private TopicsListProviderConnector topicsListProviderConnector;

    @KafkaListener(topics = "#{topicsListProviderConnector.getAll()}")
    public void listen(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String value = record.value();
        String key = record.key();

        logger.debug("Received message. topic='{}' key='{}' value='{}'", topic, key, value);
        dataHandler.handle(topic, key, value);
    }
}
