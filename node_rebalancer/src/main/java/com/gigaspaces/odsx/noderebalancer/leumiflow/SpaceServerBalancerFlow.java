package com.gigaspaces.odsx.noderebalancer.leumiflow;

import com.gigaspaces.odsx.noderebalancer.action.BaseAction;
import com.gigaspaces.odsx.noderebalancer.action.TimerAction;
import com.gigaspaces.odsx.noderebalancer.action.TransitionAction;
import com.gigaspaces.odsx.noderebalancer.admin.AdminAdapter;
import com.gigaspaces.odsx.noderebalancer.admin.action.RebalanceAction;
import com.gigaspaces.odsx.noderebalancer.admin.action.SaveContainerConfigurationAction;
import com.gigaspaces.odsx.noderebalancer.event.EventDescrirptor;
import com.gigaspaces.odsx.noderebalancer.model.Flow;
import com.gigaspaces.odsx.noderebalancer.model.State;
import com.gigaspaces.odsx.noderebalancer.model.Trigger;
import java.util.Map;

public class SpaceServerBalancerFlow extends BaseRecoveryFlow {
  private final String serverIpAddress;
  
  public enum LocalState {
    INITIAL("Initial"),
    INSTANCE_REBALANCE("InstanceRebalance"),
    END("End");
    
    public final String label;
    
    LocalState(String label) {
      this.label = label;
    }
  }
  
  public SpaceServerBalancerFlow(String name, String serverIpAddress) {
    super(name);
    this.serverIpAddress = serverIpAddress;
    getContext().setValue(AdminAdapter.AdminContextId.SELF_HOST.label, serverIpAddress);
  }
  
  public static SpaceServerBalancerFlow build(String name, String serverIpAddress, Map<String, String> parameters) {
    SpaceServerBalancerFlow flow = new SpaceServerBalancerFlow(name, serverIpAddress);
    flow.setParameters(parameters);
    State initialState = new State(LocalState.INITIAL.label);
    Trigger enteringInitialStateTrigger = new Trigger(new EventDescrirptor(EventDescrirptor.EventSpace.INTERNAL, Flow.EventCode.ENTERING_STATE.code));
    TimerAction timerAction = new TimerAction(flow.getContext(), flow.getLongParameter("waitIntervalForInitialization", Long.valueOf(180000L)).longValue());
    enteringInitialStateTrigger.addAction((BaseAction)timerAction);
    initialState.addTrigger(enteringInitialStateTrigger);
    Trigger containerCreatedTrigger = new Trigger(new EventDescrirptor(EventDescrirptor.EventSpace.ADMIN, AdminAdapter.EventCode.CONTAINER_ADDED.code));
    SaveContainerConfigurationAction saveContainerConfigurationAction = new SaveContainerConfigurationAction(flow.getContext());
    containerCreatedTrigger.addAction((BaseAction)saveContainerConfigurationAction);
    initialState.addTrigger(containerCreatedTrigger);
    Trigger timerTrigger = new Trigger(new EventDescrirptor(EventDescrirptor.EventSpace.INTERNAL, Flow.EventCode.ASYNC_TIMER_COMPLETED.code));
    timerTrigger.addAction((BaseAction)new TransitionAction(flow.getContext(), LocalState.INSTANCE_REBALANCE.label));
    initialState.addTrigger(timerTrigger);
    flow.addState(initialState);
    flow.setInitialState(initialState);
    State instanceRebalanceState = new State(LocalState.INSTANCE_REBALANCE.label);
    Trigger instanceRebalanceStateTrigger = new Trigger(new EventDescrirptor(EventDescrirptor.EventSpace.INTERNAL, Flow.EventCode.ENTERING_STATE.code));
    instanceRebalanceState.addTrigger(instanceRebalanceStateTrigger);
    RebalanceAction rebalanceAction = new RebalanceAction(flow.getContext());
    rebalanceAction.setTargetMachineCollectionKey(AdminAdapter.AdminContextId.SELF_HOST.label);
    instanceRebalanceStateTrigger.addAction((BaseAction)rebalanceAction);
    flow.addState(instanceRebalanceState);
    return flow;
  }
}
