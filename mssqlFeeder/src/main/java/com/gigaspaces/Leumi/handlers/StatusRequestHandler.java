package com.gigaspaces.Leumi.handlers;

import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

public class StatusRequestHandler extends BaseRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatusRequestHandler.class);

    public StatusRequestHandler(GigaSpace space) {

        super(space);

    }

    public Route process = (request, response) -> {
        logger.info("Status {}", StartRequestHandler.status);

        return StartRequestHandler.status;
    };

}
