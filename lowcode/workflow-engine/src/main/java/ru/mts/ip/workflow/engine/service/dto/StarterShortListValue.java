package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class StarterShortListValue {

  private UUID id;
  private String type;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  private OffsetDateTime createTime;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private UUID workflowDefinitionToStartId;
  
}
