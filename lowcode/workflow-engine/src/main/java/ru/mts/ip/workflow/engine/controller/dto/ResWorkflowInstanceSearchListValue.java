package ru.mts.ip.workflow.engine.controller.dto;

import lombok.Data;

@Data
public class ResWorkflowInstanceSearchListValue {
  private String workflowName;
  private String definitionId;
  private Long workflowVersion;
  private String tenantId;
  private String businessKey;
  private String runId;
  private String status;
  private String startTime;
  private String closeTime;
}
