package com.gigaspaces.datavalidator.controller;

import com.gigaspaces.datavalidator.model.*;
import com.gigaspaces.datavalidator.utils.EncryptionDecryptionUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.logging.Logger;

@RestController
public class AgentController {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    @PostMapping(path = "/measurement/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String runMeasurement(
            @RequestBody MeasurementRequestModel measurementRequestModel) {

        String jsonResponse = "";
        try {
            measurementRequestModel.setDataSourceHostIp(EncryptionDecryptionUtils.decrypt(measurementRequestModel.getDataSourceHostIp()));
            measurementRequestModel.setDataSourcePort(EncryptionDecryptionUtils.decrypt(measurementRequestModel.getDataSourcePort()));
            measurementRequestModel.setUsername(EncryptionDecryptionUtils.decrypt(measurementRequestModel.getUsername()));
            measurementRequestModel.setPassword(EncryptionDecryptionUtils.decrypt(measurementRequestModel.getPassword()));
            UUID uuid= UUID.randomUUID(); //Generates random UUID
            TestTask task = new TestTask(uuid.toString(),measurementRequestModel);
            task.executeTask();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
            objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
            jsonResponse= objectMapper.writeValueAsString(task);
        } catch (Exception exe) {
            exe.printStackTrace();
            jsonResponse=exe.getMessage();
        }
        return jsonResponse;
    }
}
