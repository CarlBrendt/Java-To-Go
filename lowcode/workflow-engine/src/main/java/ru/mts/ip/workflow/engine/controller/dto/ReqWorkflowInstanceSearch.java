package ru.mts.ip.workflow.engine.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import ru.mts.ip.workflow.engine.Const.WorkflowInstanceStatus;

@Data
public class ReqWorkflowInstanceSearch {
  @Schema(example = "eyJDbG9zZVRpbWUiOiIyMDI0LTA3LTE4VDA2OjEyOjIzLjY2MDA0MloiLCJTdGFydFRpbWUiOiIyMDI0LTA3LTE4VDA2OjEyOjIzLjYzNjA1MVoiLCJSdW5JRCI6ImI4NDg1MTBhLTY2ZTUtNDNjNC1iYTNhLTZjNDYwZDY3ZWIzNiJ9")
  private String pageToken;
  @Schema(minimum = "1", maximum = "100")
  private Integer pageSize;
  @Schema(example = "2024-07-18T11:33:36+03:00", format = "date-time")
  private String startingTimeFrom;
  @Schema(example = "2024-07-18T11:33:36+03:00", format = "date-time")
  private String startingTimeTo;
  private String workflowName;
  private String businessKey;
  @Schema(
    requiredMode = RequiredMode.NOT_REQUIRED, 
    allowableValues = {
      WorkflowInstanceStatus.CANCELED,
      WorkflowInstanceStatus.COMPLETED,
      WorkflowInstanceStatus.CONTINUED_AS_NEW,
      WorkflowInstanceStatus.FAILED,
      WorkflowInstanceStatus.RUNNING,
      WorkflowInstanceStatus.TERMINATED,
      WorkflowInstanceStatus.TIMED_OUT,
    }
  )
  private String executionStatus;
  
}
