package ru.mts.ip.workflow.engine.entity;

import java.util.UUID;
import org.springframework.data.domain.Persistable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "workflow_access_whitelist")
public class WorkflowAccessWhitelist implements Persistable<WorkflowAccessWhiteListId>{

  @Id
  private WorkflowAccessWhiteListId id;
  
  @Column(name = "oauth2_client_id", insertable = false, updatable = false)
  private String oauth2ClientId;

  @Column(name = "workflow_definition_id", insertable = false, updatable = false)
  private UUID workflowDefinitionId;
  
  public WorkflowAccessWhitelist() {
    
  }

  public WorkflowAccessWhitelist(WorkflowAccessWhiteListId id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return true;
  }

  @Override
  public WorkflowAccessWhiteListId getId() {
    return id;
  }

}
