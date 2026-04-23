package ru.mts.workflowscheduler.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class StarterShortListValue {

  private UUID id;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  private OffsetDateTime createTime;
  private UUID workflowDefinitionToStartId;

}
