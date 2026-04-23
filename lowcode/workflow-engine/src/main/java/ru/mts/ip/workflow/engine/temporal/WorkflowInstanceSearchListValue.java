package ru.mts.ip.workflow.engine.temporal;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowInstanceSearchListValue {
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
