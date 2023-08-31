package com.gigaspaces.Leumi.handlers;

import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

public class StopRequestHandler extends BaseRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(StopRequestHandler.class);

    public StopRequestHandler(GigaSpace space) {
        super(space);
    }

    public Route process = (request, response) -> {
        StartRequestHandler.stop();

        return "OK";
    };

}
