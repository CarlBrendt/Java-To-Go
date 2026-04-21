package ru.mts.ip.workflow.engine.dto;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(Include.NON_NULL)
public class WorkflowAccessList {
  private List<AccessEntry> accessEntries;
  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class AccessEntry {
    private UUID workflowId;
    private String oauth2ClientId;
  }
  
}
