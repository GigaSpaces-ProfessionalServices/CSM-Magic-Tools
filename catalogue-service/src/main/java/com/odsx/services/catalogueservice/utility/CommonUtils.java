package com.odsx.services.catalogueservice.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommonUtils {
    private static Logger log = LoggerFactory.getLogger(CommonUtils.class);
    public String getStringRestResponse(String url){
        log.info("Entering into -> getStringRestResponse");
        RestTemplate template = new RestTemplate();
        String obj  = template.getForObject(url,String.class);
        String strResponse = obj.toString();
        log.info("Exiting from -> getStringRestResponse");
        return strResponse;
    }

    public String getJsonStringValue(JsonObject jsonObject, String paramName){
        log.info("Entering into -> getJsonParamValue");
        log.debug("JSON Param Name -> "+paramName);
        String value = jsonObject.get(paramName).getAsString();
        log.debug("JSON Param value -> "+value);
        log.info("Exiting from -> getJsonParamValue");
        return value;
    }

    public List getJsonArrayValue(JsonObject jsonObject, String paramName){
        List list = new ArrayList();
        JsonArray metadataArray = jsonObject.getAsJsonArray("metadata").getAsJsonArray();
        log.info("Metadata Array -> " + metadataArray.toString());
        for (JsonElement jsonElement : metadataArray) {
            list.add(jsonElement.getAsString());
        }
        return list;
    }


}
