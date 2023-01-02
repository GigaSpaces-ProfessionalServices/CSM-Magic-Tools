package com.gigaspaces.connector.kafka.Learning;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.MetadataProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;

@Component
@Profile(Consts.LEARNING_MODE)
public class TopicsListProviderLearning {

    private static final Logger logger = LoggerFactory.getLogger(TopicsListProviderLearning.class);

    @Autowired
    private MetadataProviderConfig metadataProviderConfig;

    private HashSet<String> topics = new HashSet<>();

    @PostConstruct
    public void initialize() {
        topics.addAll(metadataProviderConfig.getTopics());

        logger.info("Initialization finished. Topics list: {}", String.join(",", topics));
    }

    public String[] getAll() {
        return topics.toArray(new String[0]);
    }
}
