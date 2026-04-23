package ru.mts.workflowmail.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class ReqStartWorkflow {

  private ReqRef workflowRef;
  private ReqWorkflowStartConfig workflowStartConfig;

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ReqWorkflowStartConfig {
    private String businessKey;
    private JsonNode variables;
    private Duration taskTimeout;
    private Duration runTimeout;
    private Duration executionTimeout;
  }
}
