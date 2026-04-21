package ru.mts.ip.workflow.engine.service;

import java.util.Optional;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.EventCorrelation;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchResult;

public interface WorkflowExecutionEngine {

  WorkflowExecutionResult execute(DetailedWorkflowDefinition def, WorkflowStartConfig executionConfig);

  void signal(EventCorrelation sig, Variables variables);

  Optional<WorkflowInstance> getInstance(WorkflowIstanceIdentity identity);
  
  WorkflowInstanceSearchResult searchInstances(WorkflowInstanceSearch searchConfig);
  
  Long searchInstancesCount(WorkflowInstanceSearch searchConfig);
  
  InstanceHistory getInstanceHistory(WorkflowIstanceIdentity identity);

  void cancel(String businessKey);

  void terminate(String businessKey, String reason);
}
