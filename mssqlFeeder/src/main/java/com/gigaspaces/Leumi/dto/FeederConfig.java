package com.gigaspaces.Leumi.dto;

public class FeederConfig {
    private int writeBatchSize;
    private int sleepAfterWriteInMillis;
    private int progressPrintIntervalInSec;

    public FeederConfig(int writeBatchSize, int sleepAfterWriteInMillis, int progressPrintIntervalInSec) {
        this.writeBatchSize = writeBatchSize;
        this.sleepAfterWriteInMillis = sleepAfterWriteInMillis;
        this.progressPrintIntervalInSec = progressPrintIntervalInSec;
    }

    public int getWriteBatchSize() {
        return writeBatchSize;
    }

    public int getSleepAfterWriteInMillis() {
        return sleepAfterWriteInMillis;
    }

    public int getProgressPrintIntervalInSec() {
        return progressPrintIntervalInSec;
    }

}
