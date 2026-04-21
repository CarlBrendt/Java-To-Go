package ru.mts.ip.workflow.engine.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;

import java.util.Collection;

@Data
public class IbmmqConnection implements EncryptedDataHandler {
  private IbmmqAuth authDef;
  private String addresses;
  private Integer ccsid;
  private String queueManager;
  private String channel;

  @Override
  public void applySecrets(Collection<ClientErrorDescription> errorPouch,
      ExternalProperties resolved) {
    resolved.applySecret(this::getAddresses, errorPouch::add, this::setAddresses);
    if (authDef != null) {
      authDef.applySecrets(errorPouch, resolved);
    }
  }

  public void removeCredentials() {
      if (authDef != null) {
        authDef.removeCredentials();
      }
  }
}
