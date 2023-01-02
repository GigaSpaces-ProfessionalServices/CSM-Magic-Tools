package com.gs.leumi.adabase;
import com.google.common.collect.ImmutableList;
import com.gs.leumi.adabase.config.Configuration;
import com.gs.leumi.adabase.parser.Parser;
import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Set;

@Component
@ConfigurationProperties("mq")
public class MQClient extends BaseClient{
    private static final Logger logger = LoggerFactory.getLogger(MQClient.class);

    private String hostname;
    private String channel;
    private String qManager;
    private String queueName;
    private String sslChipherSuite;
    private int port;
    private int maxEvents;
    MQQueueManager _queueManager;
    @Autowired
    private AdminClient adminClient;
    @Autowired
    private Configuration config;

    public MQClient(){

    }

    private void init(){
        MQEnvironment.hostname = hostname;
        MQEnvironment.channel = channel;
        MQEnvironment.port = port;
        MQEnvironment.sslCipherSuite = sslChipherSuite;
    }


    public void run() throws Exception{
        init();
        boolean topicIsReady= false;
        while (!topicIsReady) {
            try {
                ListTopicsResult topics = adminClient.listTopics();
                topicIsReady = true; //if we got this far; Kafka is there and the topics are created.
                logger.info("Kafka is ready ");
            }catch (Exception e){
                logger.warn("check if Kafka is running", e);
            }
        }

        try {
            selectQMgr();
        } catch (MQException e) {
            logger.error(String.format("Couldn't select qManager %s\n channel %s\n queue name %s\n ssl chipersuite %s\n hostname %s\n",
                    qManager, channel, queueName, sslChipherSuite, hostname), e);
            throw e;
        }
        int openOptions = MQC.MQOO_INQUIRE + MQC.MQOO_FAIL_IF_QUIESCING + MQC.MQOO_INPUT_SHARED;
        boolean stopRun = false;
        while (!(stopRun || Parser.STOP)) {
            try {
                MQQueue queue = _queueManager.accessQueue(queueName,
                        openOptions,
                        null, // default q manager
                        null, // no dynamic q name
                        null); // no alternate user id
                MQGetMessageOptions getOptions = new MQGetMessageOptions();
//                getOptions.options = MQC.MQGMO_NO_WAIT + MQC.MQGMO_FAIL_IF_QUIESCING + MQC.MQGMO_SYNCPOINT;
                getOptions.options = MQC.MQGMO_WAIT + MQC.MQGMO_FAIL_IF_QUIESCING + MQC.MQGMO_SYNCPOINT;
                getOptions.waitInterval=60000;
                int eventsCounter = maxEvents;
                logger.info("Reading from MQ total of " + maxEvents);
                    while (eventsCounter != 0 && !(stopRun || Parser.STOP)) {
                        try {
                            MQMessage message = new MQMessage();

                            queue.get(message, getOptions);
                            byte[] b = new byte[message.getMessageLength()];
                            message.readFully(b);
                            String event = new String(b);
                            if (eventsCounter > 0)
                                eventsCounter--;
                            if (logger.isDebugEnabled()) logger.debug(event);
                            if (parser.consumeEvent(event)) {
                                _queueManager.commit();
                            }
                        } catch(MQException e){
                            if ((e.completionCode == CMQC.MQCC_FAILED) &&
                                    (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE)) {
                                try {
                                    _queueManager.commit();
                                } catch (MQException ee){
                                    logger.error("Cannot commit transaction");
                                }

                                // All messages read.
                                logger.info("All messages were read, queue is empty. " + parser.toString());
//                                Thread.sleep(10000);
                            }
                        }
                    }
                    stopRun = true;
                    _queueManager.commit();
                    logger.info("\neventsCounter: " + eventsCounter + " stopRun: " + stopRun + "STOP: " + Parser.STOP);
                    logger.info("Requested messages were read: " + parser.toString());
            } catch(MQException e){
                if ((e.completionCode == CMQC.MQCC_FAILED) &&
                        (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE)) {
                    try {
                        _queueManager.commit();
                    } catch (MQException ee){
                        logger.error("Cannot commit transaction");
                    }

                    // All messages read.
                    logger.info("All messages were read, queue is empty. " + parser.toString());
                    //Thread.sleep(10000);
                }
            } catch(IOException e){
                logger.error("Got Exception: ", e);
            } catch(SAXException e){
                logger.error("Got XML parse Exception: ", e);
            } catch(ParserConfigurationException e){
                logger.error("Got XML parser config Exception: ", e);
            }
        }
        logger.info("\nAbout to exit. stopRun: " + stopRun + "STOP: " + Parser.STOP);
    }

    private void selectQMgr() throws MQException
    {
        _queueManager = new MQQueueManager(qManager);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getqManager() {
        return qManager;
    }

    public void setqManager(String qManager) {
        this.qManager = qManager;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getSslChipherSuite() {
        return sslChipherSuite;
    }

    public void setSslChipherSuite(String sslChipherSuite) {
        this.sslChipherSuite = sslChipherSuite;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
    }

}
