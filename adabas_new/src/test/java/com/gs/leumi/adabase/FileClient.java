package com.gs.leumi.adabase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;

@Component
@ConfigurationProperties("test")
public class FileClient extends BaseClient {

    private String path;

    @Override
    public void run() throws Exception {



        BufferedReader reader = new BufferedReader(new FileReader(path));
        for(String line = reader.readLine(); line!=null; line=reader.readLine()){
            parser.consumeEvent(line);
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
