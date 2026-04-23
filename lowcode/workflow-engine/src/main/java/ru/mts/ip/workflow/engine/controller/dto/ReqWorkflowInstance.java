package ru.mts.ip.workflow.engine.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReqWorkflowInstance {
  private String businessKey;
  @NotEmpty
  private String tenantId;
  @NotEmpty
  private String definitionName;
}
