package ru.mts.ip.workflow.engine.controller.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ResDefinitionListValue {
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
