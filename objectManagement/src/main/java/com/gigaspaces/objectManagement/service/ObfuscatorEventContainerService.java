package com.gigaspaces.objectManagement.service;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.objectManagement.listener.MySimpleListener;
import com.gigaspaces.objectManagement.model.ObfuscatorEventContainerDetail;
import com.gigaspaces.objectManagement.utils.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitInstanceStatistics;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventContainerServiceMonitors;
import org.openspaces.events.asyncpolling.AsyncPollingEventContainerServiceMonitors;
import org.openspaces.events.notify.NotifyEventContainerServiceMonitors;
import org.openspaces.events.polling.PollingEventContainerServiceMonitors;
import org.openspaces.events.polling.SimplePollingContainerConfigurer;
import org.openspaces.events.polling.SimplePollingEventListenerContainer;
import org.openspaces.events.polling.receive.SingleReadReceiveOperationHandler;
import org.openspaces.pu.container.jee.stats.WebRequestsServiceMonitors;
import org.openspaces.remoting.RemotingServiceMonitors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObfuscatorEventContainerService {

    @Value("${polling.container.file}")
    private String obfuscatorEventContainerDetailFilePath;

    private static final Logger logger = LoggerFactory.getLogger(ObfuscatorEventContainerService.class);

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private Admin admin;
    static HashMap<String,SimplePollingEventListenerContainer> pollingEventListenerContainerMap = new HashMap<>();

    public void registerInBatch() {
        logger.info("##### ObfuscatorEventContainerService.registerInBatch() start #####");
        List<ObfuscatorEventContainerDetail> obfuscatorEventContainerDetailList = CommonUtil.getObfuscatorEventContainerDetail(obfuscatorEventContainerDetailFilePath);
        obfuscatorEventContainerDetailList.forEach(this::eventRegister);
        logger.info("##### ObfuscatorEventContainerService.registerInBatch() end #####");
    }

    private void eventRegister(ObfuscatorEventContainerDetail obfuscatorEventContainerDetail) {
        String typeName = obfuscatorEventContainerDetail.getTypeName();
        String srcPropName = obfuscatorEventContainerDetail.getSrcPropName();
        String destPropName = obfuscatorEventContainerDetail.getDestPropName();
        String obfuscatePropName = obfuscatorEventContainerDetail.getObfuscatePropName();
        String obfuscationType = obfuscatorEventContainerDetail.getObfuscationType();
        String spaceId = obfuscatorEventContainerDetail.getSpaceId();

        logger.info("type: " + typeName);
        logger.info("destPropName: " + destPropName);
        logger.info("srcPropName: " + srcPropName);
        logger.info("obfuscatePropName: " + obfuscatePropName);
        logger.info("obfuscationType: " + obfuscationType);
        logger.info("spaceId:" + spaceId);

        SingleReadReceiveOperationHandler handler = new SingleReadReceiveOperationHandler();
        handler.setNonBlocking(true);
        handler.setNonBlockingFactor(10);

        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<>(typeName, obfuscatePropName + " = ?");
        sqlQuery.setParameter(1, null);

        SimplePollingEventListenerContainer pollingContainer = new SimplePollingContainerConfigurer(gigaSpace)
                .template(sqlQuery)
                .receiveOperationHandler(handler)
                .eventListener(new MySimpleListener(typeName, srcPropName, destPropName, obfuscatePropName, obfuscationType, spaceId))
                .pollingContainer();

        logger.info("polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());
        pollingContainer.start();
        pollingEventListenerContainerMap.put(typeName+srcPropName+destPropName, pollingContainer);
        logger.info("polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());

        /*logger.info("polling name : "+pollingContainer.getName()+", status : "+pollingContainer.isActive());
        pollingContainer.destroy();
        logger.info("polling name : "+pollingContainer.getName()+", status : "+pollingContainer.isActive());*/

    }

    public void dropEventListenerBatch() {
        logger.info("##### ObfuscatorEventContainerService.dropEventListenerBatch() start #####");
        //List<ObfuscatorEventContainerDetail> obfuscatorEventContainerDetailList = CommonUtil.getObfuscatorEventContainerDetail(obfuscatorEventContainerDetailFilePath);
        //obfuscatorEventContainerDetailList.forEach(this::dropEventListener);
        pollingEventListenerContainerMap.forEach((s, pollingContainer) -> {
            logger.info("Drop : polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());
            pollingContainer.destroy();
        });
        pollingEventListenerContainerMap.clear();
        logger.info("##### ObfuscatorEventContainerService.dropEventListenerBatch() end #####");
    }

    /*private void dropEventListener(ObfuscatorEventContainerDetail obfuscatorEventContainerDetail) {
        String typeName = obfuscatorEventContainerDetail.getTypeName();
        String srcPropName = obfuscatorEventContainerDetail.getSrcPropName();
        String destPropName = obfuscatorEventContainerDetail.getDestPropName();
        String obfuscatePropName = obfuscatorEventContainerDetail.getObfuscatePropName();
        String obfuscationType = obfuscatorEventContainerDetail.getObfuscationType();
        String spaceId = obfuscatorEventContainerDetail.getSpaceId();

        logger.info("type: " + typeName);
        logger.info("destPropName: " + destPropName);
        logger.info("srcPropName: " + srcPropName);
        logger.info("obfuscatePropName: " + obfuscatePropName);
        logger.info("obfuscationType: " + obfuscationType);
        logger.info("spaceId:" + spaceId);

        SingleReadReceiveOperationHandler handler = new SingleReadReceiveOperationHandler();
        handler.setNonBlocking(true);
        handler.setNonBlockingFactor(10);

        SQLQuery<SpaceDocument> sqlQuery = new SQLQuery<>(typeName, obfuscatePropName + " = ?");
        sqlQuery.setParameter(1, null);

        SimplePollingEventListenerContainer pollingContainer = new SimplePollingContainerConfigurer(gigaSpace)
                .template(sqlQuery)
                .receiveOperationHandler(handler)
                .eventListener(new MySimpleListener(typeName, srcPropName, destPropName, obfuscatePropName, obfuscationType, spaceId))
                .pollingContainer();
        logger.info("polling name : "+pollingContainer.getName()+", status : "+pollingContainer.isActive());
        logger.info("polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());

        pollingContainer.destroy();
        logger.info("polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());

    }*/

    public JsonArray eventListenerList() throws RemoteException {
        logger.info("##### ObfuscatorEventContainerService.eventListenerList() start #####");
        JsonArray jsonArray = new JsonArray();
        if (pollingEventListenerContainerMap.isEmpty()) {
            return jsonArray;
        }
       /* pollingEventListenerContainerMap.forEach((s, pollingContainer) -> {
            logger.info("Drop : polling name : "+pollingContainer.getName()+", active : "+pollingContainer.isActive()+ ",running : "+pollingContainer.isRunning());
            pollingContainer.getServicesDetails();
        });*/

        List<ObfuscatorEventContainerDetail> obfuscatorEventContainerDetailList = CommonUtil.getObfuscatorEventContainerDetail(obfuscatorEventContainerDetailFilePath);
        for(ObfuscatorEventContainerDetail obfuscatorEventContainerDetail : obfuscatorEventContainerDetailList){
            String typeName = obfuscatorEventContainerDetail.getTypeName();
            String srcPropName = obfuscatorEventContainerDetail.getSrcPropName();
            String destPropName = obfuscatorEventContainerDetail.getDestPropName();
            String obfuscatePropName = obfuscatorEventContainerDetail.getObfuscatePropName();
            String obfuscationType = obfuscatorEventContainerDetail.getObfuscationType();
            String spaceId = obfuscatorEventContainerDetail.getSpaceId();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type",typeName);
            jsonObject.addProperty("srcPropName",srcPropName);
            jsonObject.addProperty("destPropName",destPropName);
            jsonObject.addProperty("obfuscatePropName",obfuscatePropName);
            jsonObject.addProperty("obfuscationType",obfuscationType);
            jsonObject.addProperty("spaceId",spaceId);
            jsonArray.add(jsonObject);
        }
        logger.info("##### ObfuscatorEventContainerService.eventListenerList() end #####");
        return jsonArray;
    }
}
