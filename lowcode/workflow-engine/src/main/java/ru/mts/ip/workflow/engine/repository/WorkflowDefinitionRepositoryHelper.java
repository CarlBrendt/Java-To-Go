package ru.mts.ip.workflow.engine.repository;

import java.util.Optional;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;

public interface WorkflowDefinitionRepositoryHelper {
  public Optional<WorkflowDefinition> findDeployedDefinition(Ref workflowRef);
}
