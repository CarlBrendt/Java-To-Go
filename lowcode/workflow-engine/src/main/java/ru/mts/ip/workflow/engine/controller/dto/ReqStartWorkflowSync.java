package ru.mts.ip.workflow.engine.controller.dto;

import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqStartWorkflowSync {

  @Valid
  @NotNull
  private ReqRef workflowRef;
  @Valid
  @NotNull
  private ReqWorkflowStartConfigSync workflowStartConfig;
  
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ReqWorkflowStartConfigSync {
    @NotBlank
    private String businessKey;
    private Map<String, JsonNode> variables;
    @Schema(type = "string", format = "ISO_8601_duration", example = "P365D")
    private Duration executionTimeout;
  }
  
  public static interface ValidationGroups{
    public static interface SyncStart{}
  }
  
}
