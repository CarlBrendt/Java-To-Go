package ru.mts.ip.workflow.engine.controller.dto;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ReqSapInbound {
  private ReqSapConnection connectionDef;
  private ReqRefVersionless connectionRef;
  private String name;
  private String tenantId;
  private String description;
  private Boolean enabled;
  private Map<String, JsonNode> props;
  private ReqRef workflowDefinitionToStartRef;
}
