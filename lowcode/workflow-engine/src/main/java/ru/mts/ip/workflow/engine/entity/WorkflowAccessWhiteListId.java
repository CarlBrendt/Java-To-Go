package ru.mts.ip.workflow.engine.entity;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class WorkflowAccessWhiteListId {

  @Column(name = "oauth2_client_id")
  private String oauth2ClientId;
  @Column(name = "workflow_definition_id")
  private UUID workflowDefinitionId;
  
  public WorkflowAccessWhiteListId() {
    
  }

  public WorkflowAccessWhiteListId(String oauth2ClientId, UUID workflowDefinitionId) {
    this.oauth2ClientId = oauth2ClientId;
    this.workflowDefinitionId = workflowDefinitionId;
  }
}
