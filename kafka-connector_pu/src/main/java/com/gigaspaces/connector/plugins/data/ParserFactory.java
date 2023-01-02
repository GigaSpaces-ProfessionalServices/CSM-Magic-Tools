package com.gigaspaces.connector.plugins.data;

import com.gigaspaces.connector.config.ConfigurationException;
import org.springframework.stereotype.Component;

@Component
public class ParserFactory {

    public Parser create(String messageFormat) {
        Parser parser;
        switch (messageFormat) {
            case "JSON":
                parser = new JsonParser();
                break;
            default:
                throw new ConfigurationException("Unexpected message format value: '{}'", messageFormat);
        }

        return parser;
    }
}
