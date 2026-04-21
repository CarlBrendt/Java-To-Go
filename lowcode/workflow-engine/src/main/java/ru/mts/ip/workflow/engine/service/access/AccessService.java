package ru.mts.ip.workflow.engine.service.access;

import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.WorkflowAccessList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessService {
  void appendAccessConfig(WorkflowAccessList list);
  void replaceAccessList(WorkflowAccessList list);
  List<String> getClientIds(UUID definitionId);
  boolean isPermissionsEnoughToRunWorkflow(DetailedWorkflowDefinition definition);
  Optional<Const.Errors2> findAccessTroubleToStartWorkflow(DetailedWorkflowDefinition definition);
}
