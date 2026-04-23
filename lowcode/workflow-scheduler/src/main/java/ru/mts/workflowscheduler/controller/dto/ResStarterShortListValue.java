package ru.mts.workflowscheduler.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class ResStarterShortListValue {
  private UUID id;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  @Schema(example = "2024-10-08T10:16:30.090482+03:00")
  private String createTime;
  private UUID workflowDefinitionToStartId;
}
