package com.gigaspaces.connector.data;

import com.gigaspaces.connector.plugins.data.Parser;

public class MessageContainer {
    private final String topic;
    private final Parser keyParser;
    private final Parser valueParser;
    private String propertySelectorTemplate = null;

    public MessageContainer(String topic, Parser keyParser, Parser valueParser) {
        this.topic = topic;
        this.keyParser = keyParser;
        this.valueParser = valueParser;
    }

    public String getTopic() {
        return topic;
    }

    public Parser getKeyParser() {
        return keyParser;
    }

    public Parser getValueParser() {
        return valueParser;
    }

    public void setPropertySelectorTemplate(String selector) {
        this.propertySelectorTemplate = selector;
    }

    public String getPropertySelectorTemplate() {
        return this.propertySelectorTemplate;
    }
}
