package com.odsx.services.catalogueservice.utility;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CommonUtils {

    public String getStringRestResponse(String url){
        RestTemplate template = new RestTemplate();
        String obj  = template.getForObject(url,String.class);
        String strResponse = obj.toString();
        return strResponse;
    }
}
