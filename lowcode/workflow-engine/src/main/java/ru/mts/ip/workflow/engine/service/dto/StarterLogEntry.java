package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class StarterLogEntry {
  private UUID id;
  private String name;
  private String tenantId;
  private OffsetDateTime createTime;
  private UUID workflowDefinitionToStartId;
}
