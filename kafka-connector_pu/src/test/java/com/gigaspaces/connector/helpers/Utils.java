package com.gigaspaces.connector.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.connector.data.DataHandler;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static com.gigaspaces.internal.io.BootIOUtils.getResourcePath;

public class Utils {

    public static HashMap<String, Object>[] parseJsonResource(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File from = new File(String.valueOf(getResourcePath("data/" + filename)));

            HashMap<String, Object>[] o;
            o = mapper.readerFor(HashMap[].class).readValue(from);

            return o;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void ReceiveMessages(String topic, String filename, DataHandler dataHandler) {
        HashMap<String, Object>[] stringObjectHashMap = parseJsonResource(filename);

        try {
            for (HashMap<String, Object> x : stringObjectHashMap) {
                HashMap<String, String> key = (HashMap<String, String>) x.get("key");
                String keyString;
                if (key == null) {
                    keyString = null;
                } else {
                    keyString = new ObjectMapper().writeValueAsString(key);
                }

                HashMap<String, Object> value = (HashMap<String, Object>) x.get("value");
                String valueString;
                if (value == null) valueString = null;
                else valueString = new ObjectMapper().writeValueAsString(value);

                dataHandler.handle(topic, keyString, valueString);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static void ReceiveMessages(String topic, String filename, DataPipelineConfigGenerator dataPipelineConfigGenerator) {
        HashMap<String, Object>[] stringObjectHashMap = parseJsonResource(filename);

        try {
            for (HashMap<String, Object> x : stringObjectHashMap) {
                HashMap<String, String> key = (HashMap<String, String>) x.get("key");
                String keyString = new ObjectMapper().writeValueAsString(key);

                HashMap<String, Object> value = (HashMap<String, Object>) x.get("value");
                String valueString = new ObjectMapper().writeValueAsString(value);
                dataPipelineConfigGenerator.handleMessage(topic, keyString, valueString);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
