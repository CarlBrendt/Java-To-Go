package ru.mts.ip.workflow.engine.dto;

import java.util.Optional;
import lombok.Data;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

@Data
public class SapInboundStarter {

  private Ref inboundRef;
  private SapInbound inboundDef;
  
  public void applySecrets(ExternalProperties secrets) {
    if(inboundDef != null && secrets != null) {
      inboundDef.applySecrets(secrets);
    }
  }
  
  public void removeCredentials() {
    Optional.ofNullable(inboundDef).ifPresent(SapInbound::removeCredentials);
  }
  
}
