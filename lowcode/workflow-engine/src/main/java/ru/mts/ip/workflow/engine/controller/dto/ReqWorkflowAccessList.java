package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqWorkflowAccessList {
  private List<ReqAccessEntry> accessEntries;
    
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ReqAccessEntry {
    private UUID workflowId;
    private String oauth2ClientId;
  }
  
}
