package com.gigaspaces.odsx.noderebalancer.admin.action;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.gigaspaces.odsx.noderebalancer.action.BaseAction;
import com.gigaspaces.odsx.noderebalancer.action.Status;
import com.gigaspaces.odsx.noderebalancer.admin.AdminAdapter;
import com.gigaspaces.odsx.noderebalancer.model.Context;
import com.gigaspaces.odsx.noderebalancer.model.Pair;
import org.openspaces.admin.Admin;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.machine.Machines;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.SpacePartition;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class RebalanceAction extends BaseAction {

    public RebalanceAction(Context context) {
        super(context, Type.TASK);
    }

    private String targetMachineCollectionKey;
    private String targetContainerMarkerkey;

    @Override
    public Status callInternal() throws Exception {
        //Iterate over space instances
        //Ensure there is approx 50% mix of primary and backup instances on each machine
        // Use demote action to achieve balance
        System.out.println("Running DeleteAction");
        //TODO: Context value should come from workflow / previous steps
        if(targetMachineCollectionKey == null || targetMachineCollectionKey.length() == 0){
            targetMachineCollectionKey = AdminAdapter.AdminContextId.SELF_HOST.label;
        }

        if( targetContainerMarkerkey == null || targetContainerMarkerkey.length() == 0){
            targetContainerMarkerkey = AdminAdapter.AdminContextId.NO_CONTAINER_MARKER.label;
        }

        /*
            Common use case- the rebalance is for workflow target host
         */
        if( AdminAdapter.AdminContextId.SELF_HOST.label.equals(targetMachineCollectionKey)){
            String hostIpAddress = getFlowContext().getSetServerConfiguration().getIpAddress();
            Machine machine = AdminAdapter.getAdmin().getMachines().getMachineByHostAddress(hostIpAddress) ;
            if(machine != null){
                logger.info("Beginning to rebalance containers for the workflow target machine : " + hostIpAddress);
                rebalanceContainersOnMachine(machine);
                return Status.SUCCESS;

            }else {
                logger.warning("Could not obtain machine for the workflow host, rebalance operation could not be completed : " + hostIpAddress );
                return Status.FAILURE;

            }
        }

        LinkedList<String > machineList  = (LinkedList<String>) getFlowContext().getValue(targetMachineCollectionKey);
        if(machineList == null ){
            logger.warning(getFlowContext().getContextName() + " No Machine host ip address found in context to delete containers from");
            return Status.FAILURE;
        }
        Machines machines = AdminAdapter.getAdmin().getMachines();
        for(Machine machine : machines){
            if( machineList.contains( machine.getHostAddress())){
                rebalanceContainersOnMachine(machine);
            }
        }

        return Status.SUCCESS;
    }

    private void rebalanceContainersOnMachine(Machine machine) {
        int totalSpaceInstances = 0, primaryspaceInstances = 0;
        //Pair<Integer, Integer> spaceInstanceCounts = getSpaceInstanceCounts(machine.getHostAddress());
        rebalanceSpaceContainers(machine.getHostAddress());


    }


    private Pair<Integer, Integer> getSpaceInstanceCounts(String spaceName, String machineAddress) {

        Admin admin = AdminAdapter.getAdmin();
        Space space = admin.getSpaces().waitFor(spaceName);
        int primaryInstanceCount = 0, backupSpaceInstanceCount = 0;

        for (SpaceInstance spaceInstance : space) {
            System.out.println("   -> INSTANCE [" + spaceInstance.getUid()
                    + "] instanceId [" + spaceInstance.getInstanceId()
                    + "] backupId [" + spaceInstance.getBackupId()
                    + "] Mode [" + spaceInstance.getMode() + "]");
                    //spaceInstance.getBackupId();
            System.out.println("         -> Host: "
                    + spaceInstance.getMachine().getHostAddress());
            if(spaceInstance.getMachine().getHostAddress().equals(machineAddress)){
                if(isPrimary(spaceInstance)){
                    primaryInstanceCount++;
                }else {
                    backupSpaceInstanceCount++;
                }
            }

        }

        Pair<Integer, Integer> spaceInstanceCounts = new Pair<>(primaryInstanceCount, backupSpaceInstanceCount);
        return spaceInstanceCounts;
    }

    private boolean isPrimary(SpaceInstance spaceInstance) {
        boolean isThisPrimaryInstance = false;
        SpacePartition spacePartition = spaceInstance.getPartition();

        int primaryInstanceId = spacePartition.getPrimary().getInstanceId();
        int backupInstanceId = spacePartition.getBackup().getInstanceId();
        logger.info(String.format("Space Instance id - %s, primary instance id: %s, backup instance id: %s .", spaceInstance.getInstanceId(), primaryInstanceId, backupInstanceId));
        if (spaceInstance.getInstanceId() == primaryInstanceId) {
            isThisPrimaryInstance = true;
        }
        return isThisPrimaryInstance;
    }


    private void rebalanceSpaceContainers(String hostAddress) {

        Admin admin = AdminAdapter.getAdmin();
        Space[] spaces = admin.getSpaces().getSpaces();
        /*
         *ToDo: Few assumption like it has at least one zone so please make it more robust
         */
        int totalSpaceInstances = getFlowContext().getNodeCount() * getFlowContext().getContainerCount();
        spaces[0].waitFor(totalSpaceInstances, getFlowContext().getLongParameter("waitIntervalBeforeInstanceRebalance",30*1000L), TimeUnit.MILLISECONDS);

        // We expect only one space configured
        SpaceInstance[] instances = spaces[0].getInstances();
        boolean alternate = true;

        /**
         * Commented By: Himali Sathavara
         * Desc : LEUMI-JIRA #378
         * Rebalancer did not work properly if this action tries to demote an instance that was already backup instance orignally.
         *
        for (SpaceInstance spaceInstance : instances) {
            if(spaceInstance.getMachine().getHostAddress().equals(hostAddress)){
                logger.info("Obtained Space instance for rebalancing  : " + spaceInstance);
                logger.info(" Cluster information for the space instance: " + spaceInstance.getClusterInfo());
                SpacePartition spacePartition = spaceInstance.getPartition();
                for(SpaceInstance instance : spacePartition.getInstances()){
                    if(! instance.getId().equals( spaceInstance.getId())){
                        logger.info(String.format(" Got instance to demote : %s  , %s , Host  %s ", instance.getId(),instance.getSpaceInstanceName(), instance.getMachine().getHostAddress()));
                        logger.info(" Cluster information for the demote candidate space instance: " + spaceInstance.getClusterInfo());
                        if(alternate){
                            demoteSpaceInstance(instance);;
                        }else {
                            logger.info("Skipping this instance, should demote the next one.");
                        }
                        alternate = !alternate;
                    }
                }
            }
        }*/
        /**
         * Added By: Himali Sathavara
         * Desc : LEUMI-JIRA #378
         * Rebalancer did not work properly if this action tries to demote an instance that was already backup instance orignally.
         */
        for (SpaceInstance spaceInstance : instances) {
            if(spaceInstance.getMachine().getHostAddress().equals(hostAddress)){
                logger.info("Obtained Space instance for rebalancing  : " + spaceInstance);
                logger.info("Obtained Space instanceID  : " + spaceInstance.getId());
                logger.info("Obtained Space instance name  : " + spaceInstance.getSpaceInstanceName());
                logger.info("Obtained Space backup instance name  : " + spaceInstance.getBackupId());
                logger.info("Obtained Space mode  : " + spaceInstance.getMode());
                logger.info(" Cluster information for the space instance: " + spaceInstance.getClusterInfo());

                //if((spaceInstance.getMode() == SpaceMode.BACKUP && spaceInstance.getBackupId() == 0) || (spaceInstance.getMode() == SpaceMode.PRIMARY && spaceInstance.getBackupId() == 1)) {

                    SpacePartition spacePartition = spaceInstance.getPartition();
                    logger.info("spacePartition : " + spacePartition.getPartitionId());
                    logger.info("spacePartition backup : " + spacePartition.getBackup().getSpaceInstanceName());
                    logger.info("spacePartition primary : " + spacePartition.getPrimary().getSpaceInstanceName());

                    for (SpaceInstance instance : spacePartition.getInstances()) {
                        logger.info("Other Space instanceID  : " + instance.getId());
                        logger.info("Other Space instance name  : " + instance.getSpaceInstanceName());
                        logger.info("Other Space backup instance name  : " + instance.getBackupId());
                        logger.info("Other Space mode  : " + instance.getMode());
                        if (!instance.getId().equals(spaceInstance.getId())) {
                            if(instance.getMode()==SpaceMode.PRIMARY && instance.getBackupId() ==1) {
                                logger.info(String.format(" Got instance to demote : %s  , %s , Host  %s ", instance.getId(), instance.getSpaceInstanceName(), instance.getMachine().getHostAddress()));
                                logger.info(" Cluster information for the demote candidate space instance: " + spaceInstance.getClusterInfo());
                                if (alternate) {
                                    demoteSpaceInstance(instance);
                                    //alternate = !alternate; //Commented By : Himali satahvara. DEsc: to fix the issue of imbalance primary and backup instance on host raised by Josh
                                    //System.exit(0);

                                } else {
                                    logger.info("Skipping this instance, should demote the next one.");
                                }

                            }
                        }
                    }

            }
        }

    }

    private void demoteSpaceInstance( SpaceInstance primaryInstance) {
        logger.info("Demoting the instance " + primaryInstance);
        primaryInstance.demote(15, TimeUnit.SECONDS);
        Object [] logParams = new Object[]{
                primaryInstance.getMachine().getHostAddress(),
                primaryInstance.getId()
        };
        logger.log(Level.INFO, "Demoted space instance : Machine {0} ,instance {1}", logParams);
    }


    public void setTargetMachineCollectionKey(String targetMachineCollectionKey) {
        this.targetMachineCollectionKey = targetMachineCollectionKey;
    }

    public void setTargetContainerMarker(String targetContainerMarkerkey) {
        this.targetContainerMarkerkey = targetContainerMarkerkey;
    }

}
