package ru.mts.ip.workflow.engine.service;

import java.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowStartConfig {
  private String businessKey;
  private Variables variables = new Variables();
  private Duration executionTimeout;

  public static class RestStartProperties {
    private JsonNode headers;
  }
  
}
