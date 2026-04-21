package ru.mts.ip.workflow.engine.temporal;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.temporal.common.context.ContextPropagator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowOptions.Builder;
import io.temporal.client.WorkflowQueryRejectedException;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties.WorkflowExecutionDefaultConfig;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.EventCorrelation;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepository;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowExecutionEngine;
import ru.mts.ip.workflow.engine.service.WorkflowExecutionResult;
import ru.mts.ip.workflow.engine.service.WorkflowHistory;
import ru.mts.ip.workflow.engine.service.WorkflowInits;
import ru.mts.ip.workflow.engine.service.WorkflowInstance;
import ru.mts.ip.workflow.engine.service.WorkflowIstanceIdentity;
import ru.mts.ip.workflow.engine.service.WorkflowStartConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class Temporal implements WorkflowExecutionEngine {

  private final WorkflowClient client;
  private final EngineConfigurationProperties engineConfig;
  private final DtoMapper mapper;
  private final WorkflowExecutorService workflowExecutorService;
  private final WorkflowDefinitionRepository repo;
  private final ContextPropagator mdcContextPropagator;

  @Override
  public WorkflowExecutionResult execute(DetailedWorkflowDefinition definition, WorkflowStartConfig executionConfig) {
    WorkflowOptions workflowOptions = compileWorkflowOptions(executionConfig, engineConfig);
    workflowOptions = workflowOptions.toBuilder().setMemo(definition.asMemo()).build();
    deleteUnnecessaryData(definition);
    var secrets = Optional.ofNullable(definition.getDetails()).map(DefinitionDetails::getSecrets).orElse(null);
    ExecutionContext ctx = new ExecutionContext(engineConfig, definition, null, secrets, executionConfig.getVariables());
    WorkflowStub workflow = client.newUntypedWorkflowStub(definition.getName(), workflowOptions);
    WorkflowExecution execution = workflow.start(ctx);
    var resultAsync  = workflow.getResultAsync(Variables.class);
    resultAsync.thenAccept(result -> MDC.clear());
    return new WorkflowExecutionResult(resultAsync, execution.getRunId(), execution.getWorkflowId());
  }
  
  private void deleteUnnecessaryData(DetailedWorkflowDefinition definition) {
    definition.setFlowEditorConfig(null);
    DefinitionDetails details = definition.getDetails();
    if(details != null) {
      details.setXsdValidation(null);
    }
  }

  private WorkflowOptions compileWorkflowOptions(WorkflowStartConfig executionConfig,
      EngineConfigurationProperties engineConfig) {

    WorkflowExecutionDefaultConfig config = engineConfig.getWorkflowExecutionConfig();
    Duration workflowExecutionTimeout = Optional.ofNullable(executionConfig.getExecutionTimeout())
        .orElse(Duration.ofSeconds(config.getDefaultExecutionTimeoutSeconds()));

    Builder builder = WorkflowOptions.newBuilder();
    builder.setContextPropagators(List.of(mdcContextPropagator));
    builder.setWorkflowExecutionTimeout(workflowExecutionTimeout);
    builder.setTaskQueue(Optional.ofNullable(engineConfig.getWorkflowDslQueueName()).orElse(Const.DEFAULT_WORKFLOW_QUEUE_V2));
    
    String businessKey = executionConfig.getBusinessKey();
    if (businessKey != null) {
      builder.setWorkflowId(businessKey);
    }

    return builder.build();
  }

  @Override
  public void signal(EventCorrelation sig, Variables variables) {
    WorkflowExecution execution =
        WorkflowExecution.newBuilder().setWorkflowId(sig.getBusinessKey()).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub(execution, Optional.empty());
    workflow.signal(sig.getMessageName(), variables);
  }

  @Override
  public Optional<WorkflowInstance> getInstance(WorkflowIstanceIdentity identity) {
    return getExecution(toWorkflowExecution(identity));
  }
  
  private WorkflowExecution toWorkflowExecution(WorkflowIstanceIdentity identity) {
    var builder = WorkflowExecution.newBuilder();
    Optional.ofNullable(identity.getBusinessKey()).ifPresent(builder::setWorkflowId);
    Optional.ofNullable(identity.getRunId()).ifPresent(builder::setRunId);
    return builder.build();
  }

  private Optional<WorkflowInstance> getExecution(WorkflowExecution execution) {
    WorkflowStub workflow = client.newUntypedWorkflowStub(execution, Optional.empty());
    try {
      WorkflowInits workflowInits = workflow.query(Const.QueryType.WORKFLOW_INITS, WorkflowInits.class);
      Map<String, WorkflowHistory.ExecutionStat> hist = new HashMap<>();
      for (String activityId: workflowInits.activityIds()){
        hist.put(activityId, workflow.query(Const.QueryType.ACTIVITY_HISTORY, WorkflowHistory.ExecutionStat.class, activityId));
      }
      WorkflowHistory workflowHistory = new WorkflowHistory(workflowInits.initVariables(), workflowInits.continuedVariables(), workflowInits.aggregateActivityId());
      workflowHistory.setHist(hist);

      WorkflowInstance workflowInstance = new WorkflowInstance();
      workflowInstance.setHist(workflowHistory);
      workflowInstance.setDef(findDefinition(workflowInits.definition().getId()));
       return Optional.of(workflowInstance);
    } catch (WorkflowNotFoundException ignore) {
    }
    return Optional.empty();
  }
 
  
  private WorkflowDefinition findDefinition(UUID id) {
    return repo.findById(id).orElseThrow();
  }
  
  @Override
  public WorkflowInstanceSearchResult searchInstances(WorkflowInstanceSearch searchConfig) {
    WorkflowServiceStubs service = client.getWorkflowServiceStubs();
    var status = searchConfig.getExecutionStatus();
    if(status == null) {
      var searchRes = service.blockingStub().listWorkflowExecutions(searchConfig.asListWorkflowExecutionsRequest());
      return mapper.toWorkflowInstanceSearchResult(searchRes);
    } else if (Const.WorkflowInstanceStatus.RUNNING.equals(status)){
      var openResult = service.blockingStub().listOpenWorkflowExecutions(searchConfig.asListOpenWorkflowExecutionsRequest());
      return mapper.toWorkflowInstanceSearchResult(openResult);
    } else {
      var closedResult = service.blockingStub().listClosedWorkflowExecutions(searchConfig.asListClosedWorkflowExecutionsRequest());
      return mapper.toWorkflowInstanceSearchResult(closedResult);
    }
  }
  
  @Override
  public Long searchInstancesCount(WorkflowInstanceSearch searchConfig) {
    WorkflowServiceStubs service = client.getWorkflowServiceStubs();
    var searchRes = service.blockingStub().countWorkflowExecutions(searchConfig.asCountWorkflowExecutionsRequest());
    return searchRes.getCount();
  }

  @Override
  public InstanceHistory getInstanceHistory(WorkflowIstanceIdentity identity) {
    InstanceHistory history;
    try {
      var instance = getInstance(identity).orElseThrow(() -> ClientError.instanceNotFound(identity));
      history = instance.asInstanceHistory();
    } catch (WorkflowQueryRejectedException grpcEx){
      history = null;
    }
    

    var temporalHistory = client.fetchHistory(identity.getBusinessKey(), identity.getRunId());
    var desc = client.getWorkflowServiceStubs().blockingStub()
      .describeWorkflowExecution(DescribeWorkflowExecutionRequest.newBuilder()
        .setExecution(toWorkflowExecution(identity))
        .setNamespace(client.getOptions().getNamespace())
      .build());

    TemporalHistoryHelper historyHelper = new TemporalHistoryHelper(temporalHistory, desc);
    history = Optional.ofNullable(history).orElse(historyHelper.instanceHistory());
    historyHelper.appendDetails(history);
    return workflowExecutorService.evaluateHistory(history);
  }

  @Override
  public void cancel(String businessKey) {
    WorkflowExecution execution =
        WorkflowExecution.newBuilder().setWorkflowId(businessKey).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub(execution, Optional.empty());
    workflow.cancel();
  }

  @Override
  public void terminate(String businessKey, String reason) {
    WorkflowExecution execution =
        WorkflowExecution.newBuilder().setWorkflowId(businessKey).build();
    WorkflowStub workflow = client.newUntypedWorkflowStub(execution, Optional.empty());
    workflow.terminate(reason);
  }

}
