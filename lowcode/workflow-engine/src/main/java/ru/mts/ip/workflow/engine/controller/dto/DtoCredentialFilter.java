package ru.mts.ip.workflow.engine.controller.dto;

import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;

public interface DtoCredentialFilter {
  DetailedWorkflowDefinition filter(DetailedWorkflowDefinition definition);
}
