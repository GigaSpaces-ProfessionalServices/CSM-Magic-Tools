package com.gigaspaces.retentionmanager.service;

import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy;
import com.gigaspaces.retentionmanager.utils.CommonUtils;
import com.gigaspaces.retentionmanager.utils.SpaceOperationsUtils;
import com.j_spaces.core.admin.IRemoteJSpaceAdmin;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class RetentionManagerService {
    private static final Logger log = LoggerFactory.getLogger(RetentionManagerService.class);
    @Autowired
    private ObjectRetentionPolicyService objectRetentionPolicyService;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private GigaSpace gigaSpace;

    @Autowired
    private SpaceOperationsUtils spaceOperations;

    @Autowired
    private InfluxDBService influxDBService;

    @PostConstruct
    public void loadAllSpaceObjectsToDB() throws RemoteException {
        log.info("Entering in to -> loadAllSpaceObjectsToDB");
        IRemoteJSpaceAdmin remoteAdmin =
                (IRemoteJSpaceAdmin)gigaSpace.getSpace().getAdmin();
        Object classList[] = remoteAdmin.getRuntimeInfo().m_ClassNames.toArray();
        GigaSpaceTypeManager defaultGigaSpaceTypeManager = gigaSpace.getTypeManager();
        log.info("Number of Objects in Space ->"+classList.length);
        for(Object obj : classList){
            String objectType = obj.toString();
            if(objectType!=null && !objectType.contains("java.lang.Object")) { // Ignoring default object in space
                ObjectRetentionPolicy objectRetentionPolicy = new ObjectRetentionPolicy();
                objectRetentionPolicy.setObjectType(objectType);
                objectRetentionPolicy.setActive(false);
                objectRetentionPolicy.setConstraintField("");
               /* String[] propertyTypes = defaultGigaSpaceTypeManager.getTypeDescriptor(objectType).getPropertiesTypes();
                for(int i=0;i<propertyTypes.length;i++) {

                    if(propertyTypes[i].)

                }*/
                List list = objectRetentionPolicyService.getRetentionPolicy(objectType);
                if (list.isEmpty()) {
                    objectRetentionPolicyService.addRetentionPolicy(objectRetentionPolicy);
                }
            }
        }
        log.info("Entering in to -> loadAllSpaceObjectsToDB");

    }

    public void cleanUpSpaceData(){
        String methodName="cleanUpSpaceData";
        Date currentDate = new Date();
        log.info("Entering in to -> "+methodName);
        List<ObjectRetentionPolicy> list = objectRetentionPolicyService.getAllRetentionPolicies();
        log.debug("Total Objects in Retention Policy DB -> "+(list!=null?list.size():"0"));
        if(list!=null && !list.isEmpty()) {
            for(ObjectRetentionPolicy objectRetentionPolicy : list) {
                log.info("objectRetentionPolicy.getObjectType() -> "+objectRetentionPolicy.getObjectType());
                GigaSpaceTypeManager defaultGigaSpaceTypeManager = gigaSpace.getTypeManager();

                SpaceTypeDescriptor spaceTypeDescriptor = defaultGigaSpaceTypeManager.getTypeDescriptor(objectRetentionPolicy.getObjectType());
                if(spaceTypeDescriptor!=null){

                    log.info("Cleaning up "+objectRetentionPolicy.getObjectType());
                    log.info("Retention Period for "+objectRetentionPolicy.getObjectType()+" -> "+objectRetentionPolicy.getRetentionPeriod());
                    if (objectRetentionPolicy.getRetentionPeriod() != null &&
                            objectRetentionPolicy.getRetentionPeriod() != "" &&
                            objectRetentionPolicy.getActive()==true &&
                            objectRetentionPolicy.getConstraintField()!="" &&
                            objectRetentionPolicy.getConstraintField()!=null) {
                        String duration = objectRetentionPolicy.getRetentionPeriod();
                        if(duration.trim().length()>0) {
                            String[] spacePropertiesNames = spaceTypeDescriptor.getPropertiesNames();
                            String spaceObjTypeOfConstraintField = "";
                            for (int i = 0; i < spacePropertiesNames.length; i++) {
                                if (spacePropertiesNames[i].equalsIgnoreCase(objectRetentionPolicy.getConstraintField())) {
                                    spaceObjTypeOfConstraintField = spaceTypeDescriptor.getPropertiesTypes()[i];
                                    break;
                                }
                            }
                            String durationAmount = duration.substring(0, duration.length() - 1);
                            String durationUnit = duration.substring(duration.length() - 1, duration.length());

                            Date dateParam = commonUtils.addSubstractFromDate(currentDate, -Integer.valueOf(durationAmount), durationUnit);
                            Object obj = convertDateToSpaceObjType(dateParam, spaceObjTypeOfConstraintField);
                            log.info(methodName, "Deleting objects prior to  ->" + dateParam);
                            deleteFromSpaceObject(spaceTypeDescriptor, objectRetentionPolicy.getConstraintField(), obj);
                        }
                    }

                } else{
                    log.info("Space object for"+objectRetentionPolicy.getObjectType()+" is not found");
                }
            }
        }
        log.info("Exiting from -> "+methodName);
    }

    public Object convertDateToSpaceObjType(Date dateParam,String spaceObjTypeOfConstraintField){
        Object obj = null;
        switch (spaceObjTypeOfConstraintField){
            case "java.lang.Long":
                obj = dateParam.getTime();
                break;
            case "java.time.Instant":
                obj = dateParam.toInstant();
                break;
            case "java.time.LocalDateTime":
                obj = dateParam.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
                break;
            case "java.sql.Date":
                obj = new java.sql.Date(dateParam.getTime());
                break;
            case "java.sql.Timestamp":
                obj = new Timestamp(dateParam.getTime());
                break;
            default:
                obj = dateParam;
                break;
        }


        return  obj;
    }
    public void deleteFromSpaceObject(SpaceTypeDescriptor spaceTypeDescriptor, String constraintField, Object obj){

        String methodName="deleteFromSpaceObject";
        Date cleanupStarted = new Date();

        log.info("Deleting from "+spaceTypeDescriptor.getTypeName());
        log.info("Deleting objects prior to "+obj.getClass().getTypeName());
        String sql = "delete from \"" + spaceTypeDescriptor.getTypeName() + "\" where "+constraintField+" < ? ";
        int objsUpdated = spaceOperations.executeSqlUpdate(sql, obj);
        log.info("Number of deleted objects from "+spaceTypeDescriptor.getTypeName()+" are "+objsUpdated);

        if(influxDBService.verifyConnection()){
            influxDBService.writeMeasurement(spaceTypeDescriptor.getTypeName(),objsUpdated,cleanupStarted);
        }

    }

}