package com.gs.csm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;

import static com.gs.csm.LoadCSVDataFromPu.loadCsvData;

/*
 * @author Aharon
 */
public class DataFeeder {
    private final Log log = LogFactory.getLog(DataFeeder.class);
    @GigaSpaceContext(name = "gigaSpace")
    private GigaSpace gigaSpace;

    Thread t=null;

    public void setNumberOfIterations(int numberOfIterations) {
        GsFactory.numberOfIterations = numberOfIterations;
    }

    public void setCsvFileName(String csvFileName) {
        GsFactory.csvFile = csvFileName;
    }

    public void setCsvPojo(String csvPojo) {
        GsFactory.csvPojo = csvPojo;
    }

    @PostConstruct
    public void construct() throws IOException, ClassNotFoundException {
        System.out.println("--- STARTING LOADING CSV FILE [" + GsFactory.csvFile+ "]");
        System.out.println("--- STARTING FEEDER WITH POJO [" + GsFactory.csvPojo+ "]");
        log.info("Starting DataFeeder");
        Thread t = new Thread(new CSVFeederExecuter());
        t.start();
    }

    @PreDestroy
    public void destroy() {
        System.out.println("--- KILLING CSV FEEDER Thread " + t);
        t.interrupt();
        t = null;
    }

    private class CSVFeederExecuter implements Runnable {
        public void run() {
            try {
                InputStream inputStream=null;
                for (int i = 1; i< GsFactory.numberOfIterations; i++) {
                    System.out.println("--- STARTING ITEARTION NUMBER " + i);
                    inputStream = LoadCSVData.class.getClassLoader().getResourceAsStream(GsFactory.csvFile);
                    loadCsvData(gigaSpace, inputStream);
                    inputStream.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                log.error("DataFeeder.CSVFeederExecuter has failed");
            }
        }
    }

}
