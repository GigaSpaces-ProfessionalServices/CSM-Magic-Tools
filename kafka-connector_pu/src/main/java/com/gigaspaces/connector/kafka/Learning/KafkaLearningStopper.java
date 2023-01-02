package com.gigaspaces.connector.kafka.Learning;

import com.gigaspaces.connector.config.Consts;
import com.gigaspaces.connector.config.DataPipelineConfig;
import com.gigaspaces.connector.metadata.DataPipelineConfigGenerator;
import com.gigaspaces.connector.utils.DataPipelineDefinitionFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Profile(Consts.LEARNING_MODE)
public class KafkaLearningStopper {
    private static final Logger logger = LoggerFactory.getLogger(KafkaLearningStopper.class);

    @Autowired
    private DataPipelineDefinitionFileWriter dataPipelineDefinitionFileWriter;

    @Autowired
    private DataPipelineConfigGenerator dataPipelineConfigGenerator;

    @Autowired
    private ConfigurableApplicationContext appContext;

    IdleTimeWatcher idleTimeWatcher = null;

    public void kafkaMessageStart() {
        if (idleTimeWatcher != null)
            idleTimeWatcher.processingMessage = true;
    }

    public void kafkaMessageEnd() {
        startWatcherIfNotRunning();
        idleTimeWatcher.processingMessage = false;
        idleTimeWatcher.idleSince = LocalTime.now();
    }

    private void startWatcherIfNotRunning() {
        if (idleTimeWatcher != null) return;

        idleTimeWatcher = new IdleTimeWatcher();
        ExecutorService executorService =
                new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());

        executorService.execute(idleTimeWatcher);
    }

    private void exitApplication() {
        DataPipelineConfig spaceTypesDefinitions = dataPipelineConfigGenerator.getDataPipelineConfig();
        dataPipelineDefinitionFileWriter.writeFile(spaceTypesDefinitions);

        logger.info("Terminating. It is ok to stop the process with Ctrl-c...");
        appContext.close();
    }

    class IdleTimeWatcher implements Runnable {

        boolean processingMessage = false;
        LocalTime idleSince = null;
        boolean terminated = false;

        @Override
        public void run() {
            while (!terminated) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (processingMessage) continue;

                if (LocalTime.now().minusSeconds(2).isAfter(idleSince)) {
                    terminated = true;
                    exitApplication();
                }
            }
        }
    }

}
