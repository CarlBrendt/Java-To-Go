package ru.mts.ip.workflow.engine.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class ResStarterShortListValue {
  private UUID id;
  private String type;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  @Schema(example = "2024-10-08T10:16:30.090482+03:00")
  private String createTime;
  @Schema(example = "2024-10-08T10:16:30.090482+03:00")
  private String startDateTime;
  @Schema(example = "2024-10-08T10:16:30.090482+03:00")
  private String endDateTime;
  private UUID workflowDefinitionToStartId;
  
}
