package com.gigaspaces.connector.data;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.plugins.data.Parser;
import com.gigaspaces.connector.plugins.data.ParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
//@Profile(Consts.CONNECTOR_MODE)
public class MessageContainerFactory {

    @Autowired
    ParserFactory parserFactory;

    @Autowired
    DataPipelineConfig dataPipelineConfig;

    public MessageContainer create(String topic, Object key, Object value) {
        String format = dataPipelineConfig.getDataFormat();

        // TODO: 21/11/2021 should not assume key and value always have the same format
        Parser keyParser;
        if (key == null)
            keyParser = null;
        else {
            keyParser = parserFactory.create(format);
            keyParser.parse(key);
        }

        Parser valueParser = parserFactory.create(format);
        valueParser.parse(value);

        MessageContainer messageContainer = new MessageContainer(topic, keyParser, valueParser);
        return messageContainer;
    }
}
