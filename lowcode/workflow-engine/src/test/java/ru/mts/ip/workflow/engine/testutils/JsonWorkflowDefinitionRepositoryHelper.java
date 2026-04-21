package ru.mts.ip.workflow.engine.testutils;

import java.util.Optional;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;

public class JsonWorkflowDefinitionRepositoryHelper implements WorkflowDefinitionRepositoryHelper{

  @Override
  public Optional<WorkflowDefinition> findDeployedDefinition(Ref workflowRef) {
    return new JsonWorkflowDefinitionRepository().findFirstByNameOrderByVersionDesc(workflowRef.getName());
  }

}
