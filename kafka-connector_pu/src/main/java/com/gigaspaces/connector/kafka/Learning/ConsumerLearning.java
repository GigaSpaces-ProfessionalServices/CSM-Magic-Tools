package com.gigaspaces.connector.kafka.Learning;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Profile(Consts.LEARNING_MODE)
public class ConsumerLearning {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerLearning.class);

    @Autowired
    private TopicsListProviderLearning topicsListProviderLearning;

    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;

    @Autowired
    private KafkaLearningStopper kafkaLearningStopper;

    @KafkaListener(topics = "#{topicsListProviderLearning.getAll()}")
    public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
        kafkaLearningStopper.kafkaMessageStart();

        String topic = record.topic();
        String value = record.value();
        String key = record.key();

        logger.debug("Received message. topic='{}' key='{}' value='{}'", topic, key, value);
        dataPipelineConfigGenerator.handleMessage(topic, key, value);

        kafkaLearningStopper.kafkaMessageEnd();
    }
}
