package com.gigaspaces.objectManagement.listener;

import com.gigaspaces.client.ChangeResult;
import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.objectManagement.utils.IdNoObfuscator;
import com.gigaspaces.query.IdQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.SpaceDataEventListener;
import org.springframework.transaction.TransactionStatus;

public class MySimpleListener implements SpaceDataEventListener {

    private static final Log logger = LogFactory.getLog(MySimpleListener.class);
    private IdNoObfuscator idNoObfuscator;
    private String typeName;
    private String srcPropName;
    private String destPropName;
    private String obfuscatePropName;
    private String obfuscationType;
    private String spaceId;


    public MySimpleListener(String typeName, String srcPropName, String destPropName, String obfuscatePropName, String obfuscationType, String spaceId) {

        this.typeName = typeName;
        this.srcPropName = srcPropName;
        this.destPropName = destPropName;
        this.obfuscatePropName = obfuscatePropName;
        this.obfuscationType = obfuscationType;
        this.spaceId = spaceId;
    }

    @Override
    public void onEvent(Object data, GigaSpace gigaSpace, TransactionStatus transactionStatus, Object source) {

        logger.debug("##### MySimpleListener.onEvent() start #####");

        // the srcProp (pnimi) is always Integer
        // the destProp (idno/passport) is always a String
        SpaceDocument spaceDocument = (SpaceDocument) data;
        Integer srcPropVal = spaceDocument.getProperty(this.srcPropName);
        String destPropVal = spaceDocument.getProperty(this.destPropName);
        Boolean isObfuscateVal = spaceDocument.getProperty(this.obfuscatePropName);

        logger.debug("typeName: " + typeName);
        logger.debug("srcPropName: " + srcPropName);
        logger.debug("srcPropVal: " + srcPropVal);
        logger.debug("destPropName: " + destPropName);
        logger.debug("destPropVal: " + destPropVal);
        logger.debug("obfuscationType: " + obfuscationType);
        logger.debug("isObfuscateName: " + obfuscatePropName);
        logger.debug("isObfuscateVal: " + isObfuscateVal);
        logger.debug("spaceId: " + spaceId);

        try {
            logger.debug("##### 1 srcPropVal ##### " + srcPropVal);
            if (srcPropVal != null) {

                logger.debug("##### 2 srcPropVal ##### " + srcPropVal);

                idNoObfuscator = new IdNoObfuscator(srcPropVal);

                String newIdNo = null;

                if (obfuscationType.equals("obfuscatToPnimi9Digits")) {
                    newIdNo = idNoObfuscator.obfuscatToPnimi9Digits();
                } else if (obfuscationType.equals("obfuscatToPnimi14Digits")) {
                    newIdNo = idNoObfuscator.obfuscatToPnimi14Digits();
                } else {
                    logger.error("obfuscationType: " + obfuscationType + " is not supported");
                }

                try {
                    IdQuery<SpaceDocument> idQuery = new IdQuery<SpaceDocument>(typeName, spaceDocument.getProperty(spaceId));
                    ChangeResult changeResult1 = gigaSpace.change(idQuery, new ChangeSet().set(destPropName, newIdNo));

                    if (changeResult1 != null) {

                        logger.debug("changed destPropVal from " + destPropVal + " to srcPropVal " + newIdNo);
                        logger.debug("##### number of changed entries #####" + changeResult1.getNumberOfChangedEntries());

                    } else {
                        logger.debug("##### changeResult1 no change #####");
                    }

                    ChangeResult changeResult2 = gigaSpace.change(idQuery, new ChangeSet().set(obfuscatePropName, Boolean.TRUE));

                    if (changeResult2 != null) {
                        logger.debug("##### changeResult2.getNumberOfChangedEntries() #####" + changeResult2.getNumberOfChangedEntries());
                    } else {
                        logger.debug("##### changeResult2 no change #####");
                    }

                } catch (Throwable e) {
                    logger.error("change failed ", e);
                }
            } else {
                logger.debug("srcPropVal in null for destPropVal " + destPropVal);
            }
        } catch (Exception e) {
            logger.error("Failed to obfuscate destPropVal " + destPropVal + " to srcPropVal " + srcPropVal + " error: ", e);
        }

        //logger.debug("##### MySimpleListener.onEvent() end #####");
        logger.debug("##### MySimpleListener.onEvent() end #####");
    }
}
