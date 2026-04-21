package ru.mts.ip.workflow.engine.controller.dto;

import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class ReqStartWorkflow {

  @Valid
  @NotNull
  private ReqRef workflowRef;
  @Valid
  @NotNull
  private ReqWorkflowStartConfig workflowStartConfig;
  
  @Data
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Accessors(chain = true)
  public static class ReqWorkflowStartConfig {
    @NotBlank
    private String businessKey;
    private Map<String, JsonNode> variables;
    @Schema(type = "string", format = "ISO_8601_duration", example = "P365D")
    private String executionTimeout;
  }
  
  public static interface ValidationGroups{
    public static interface SyncStart{}
  }
  
}
