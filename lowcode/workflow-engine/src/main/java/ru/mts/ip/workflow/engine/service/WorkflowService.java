package ru.mts.ip.workflow.engine.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobSaveOptions;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.EventCorrelation;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchListValue;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchResult;

public interface WorkflowService {
  
  WorkflowDefinition createDraftDefinition(WorkflowDefinition definition);

  WorkflowDefinition replaceDraftDefinition(UUID uuid, WorkflowDefinition definition);

  WorkflowDefinition deploy(WorkflowDefinition definition, boolean removeDraftOnSuccess);

  WorkflowDefinition findDraftById(UUID id);

  WorkflowDefinition findDeployedById(UUID id);
  
  Optional<WorkflowDefinition> findById(UUID id);

  Optional<WorkflowDefinition> findDeployedDefinition(Ref workflowRef);
  Optional<DetailedWorkflowDefinition> findExecutableDefinition(Ref workflowRef);

  WorkflowExecutionResult start(DetailedWorkflowDefinition definition, WorkflowStartConfig executionConfig);

  void signal(EventCorrelation signal, Variables variables);

  Optional<WorkflowInstance> getInstance(WorkflowIstanceIdentity identity);
  
  List<DefinitionListValue> searchDefinitions(DefinitionSearching searchConfig);
  Long searchDefinitionsCount(DefinitionSearching searchConfig);
  
  WorkflowInstanceSearchResult searchInstances(WorkflowInstanceSearch searchConfig);
  Long searchInstancesCount(WorkflowInstanceSearch searchConfig);
  
  InstanceHistory getInstanceHistory(WorkflowIstanceIdentity identity);
  
  void optimizeVariables(Variables startConfig, BlobSaveOptions blobSaveOptions);

  List<WorkflowInstanceSearchListValue> stop(Ref ref, String businessKey, boolean terminate);

  DetailedWorkflowDefinition decommissionById(UUID id);

  ResDefinitionInspection inspectDefinition(WorkflowDefinition def);
}
