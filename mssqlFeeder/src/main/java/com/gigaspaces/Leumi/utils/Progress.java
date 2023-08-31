package com.gigaspaces.Leumi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class Progress {
    private static final Logger logger = LoggerFactory.getLogger(Progress.class);

    private final int progressPrintIntervalInSec;
    private int count;
    private int processed = 0;

    public Progress(int progressPrintIntervalInSec) {
        this.progressPrintIntervalInSec = progressPrintIntervalInSec;
    }

    public void startOver(int count) {
        this.processed = 0;
        this.count = count;
    }

    public void increment(int value) {
        this.processed += value;
    }

    public int getProgressInPercents() {
        if (count == 0) {
            return 0;
        } else {
            return Math.round(100 * processed / count);
        }
    }

    private Date lastPrinted = new Date(0);
    public void printProgress() {
        Date now = new Date();
        long diffInSec = (now.getTime() - lastPrinted.getTime()) / 1000;
        if (diffInSec > progressPrintIntervalInSec) {
            logger.info("Progress {}%", getProgressInPercents());
            lastPrinted = now;
        }
    }

    public void printProgressUnconditionally() {
        logger.info("Progress {}%", getProgressInPercents());
    }
}
