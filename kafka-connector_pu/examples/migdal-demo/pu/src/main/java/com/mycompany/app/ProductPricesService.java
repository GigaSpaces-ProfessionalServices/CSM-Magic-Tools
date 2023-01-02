/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycompany.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.document.SpaceDocument;
import com.j_spaces.core.client.SQLQuery;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

@Component
@EventDriven
@Polling
public class ProductPricesService {
    private static final Logger logger = LoggerFactory.getLogger(ProductPricesService.class);
    private static final String TYPE_PRODUCTS = "testDB.dbo.products";
    private static final String TYPE_PRICES = "prices";
    private static final String PROCESSED_PROPERTY = "processed";
    private Producer<String, String> producer;
    @Resource
    private GigaSpace gigaSpace;
    @Value("${kafka.topic}")
    private String topic;
    @Value("${kafka.bootstrap}")
    private String bootstrapServers;

    @PostConstruct
    public void initialize() {
        logger.info("{} entries in space '{}'", gigaSpace.count(null), gigaSpace.getSpaceName());
        producer = createProducer();
    }

    @EventTemplate
    public SQLQuery<SpaceDocument> unprocessedData() {
        SQLQuery<SpaceDocument> template =
                new SQLQuery<>(TYPE_PRODUCTS, PROCESSED_PROPERTY + " is null");
        return template;
    }

    @SpaceDataEvent
    public SpaceDocument processData(SpaceDocument product) {
        int productId = product.getProperty("id");
        logger.info("Got event for product id = {}", productId);

        SQLQuery<Object> pricesQuery =
                new SQLQuery<>(TYPE_PRICES, "Product_ID = ? AND Country_Code = ?")
                        .setParameter(1, productId)
                        .setParameter(2, "IL");
        SpaceDocument priceDoc = (SpaceDocument) gigaSpace.read(pricesQuery);

        if (priceDoc != null) {
            Double price = priceDoc.getProperty("Price");
            String productName = product.getProperty("name");
            sendToKafka(productId, productName, price);
            logger.info("Sent to Kafka. Price = {}", price);
        }
        product.setProperty(PROCESSED_PROPERTY, true);
        return product;
    }

    @PreDestroy
    public void close() {
        logger.info("Closing");
    }

    private void sendToKafka(int productId, String productName, Double price) {
        HashMap<String, Object> productWithPrice = new LinkedHashMap<>();
        productWithPrice.put("ID", productId);
        productWithPrice.put("Name", productName);
        productWithPrice.put("Price", price);

        try {
            final ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    Integer.toString(productId),
                    new ObjectMapper().writeValueAsString(productWithPrice)
            );
            producer.send(record);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize record with id {}", productId, e);
        }
    }

    private Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}
