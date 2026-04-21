package ru.mts.ip.workflow.engine.service;

import lombok.Data;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory.ActivityExecutionState;
import ru.mts.ip.workflow.engine.temporal.WorkflowConsumedMessages;

@Data
public class WorkflowInstance {

  private WorkflowDefinition def;
  private WorkflowHistory hist;
  private WorkflowConsumedMessages consumedMessages;
  
  public InstanceHistory asInstanceHistory() {
    InstanceHistory res = new InstanceHistory();
    res.setInitVariables(hist.getInitVariables().asNode());
    res.setContinuedVariables(hist.getContinuedVariables().asNode());
    res.setWorkflowDefinition(def);
    var stats = hist.getHist().values();
    stats.stream().map(ActivityExecutionState::new).forEach(res::addActivityExecutionStat);
    return res;
  }

}
