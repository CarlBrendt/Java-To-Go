package ru.mts.ip.workflow.engine.dto;

import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

@Data
public class SapInbound {
  private SapConnection connectionDef;
  private Ref connectionRef;
  private String name;
  private String tenantId;
  private String description;
  private Boolean enabled;
  private Map<String, JsonNode> props;
  private Ref workflowDefinitionToStartRef;
  
  public void applySecrets(ExternalProperties secrets) {
    if(connectionDef != null) {
      connectionDef.applySecrets(secrets);
    }
  }
  
  public void removeCredentials() {
    Optional.ofNullable(connectionDef).ifPresent(SapConnection::removeCredentials);
  }
  
}
