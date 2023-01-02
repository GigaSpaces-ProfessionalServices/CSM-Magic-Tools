package com.gigaspaces.connector;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringBootApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

}