package ru.mts.ip.workflow.engine.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinition.ResDefinitionDetails;

@Data
@JsonInclude(Include.NON_NULL)
public class ResExecutableWorkflow {
  
  private UUID id;
  private String type;
  private String name;
  private String description;
  private String tenantId;
  private OffsetDateTime createTime;
  private Integer version;
  private String status;
  private String availabilityStatus;

  private ResDefinitionDetails details;
  private JsonNode compiled;
  private JsonNode flowEditorConfig;

}
