package ru.mts.ip.workflow.engine.service;

import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DefinitionListValue {
  private UUID id;
  private String type;
  private String name;
  private String description;
  private String tenantId;
  private String createTime;
  private String changeTime;
  private Integer version;
  private String status;
  private String ownerLogin;
}
