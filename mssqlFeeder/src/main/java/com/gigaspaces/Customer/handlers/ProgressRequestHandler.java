package com.gigaspaces.Customer.handlers;

import com.gigaspaces.Customer.utils.Progress;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

public class ProgressRequestHandler extends BaseRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProgressRequestHandler.class);
    private Progress progress;

    public ProgressRequestHandler(GigaSpace space, Progress progress) {
        super(space);
        this.progress = progress;
    }

    public Route process = (request, response) -> {
        int progressInPercents = progress.getProgressInPercents();
        logger.info("Progress {}%", progressInPercents);

        return progressInPercents;
    };

}
