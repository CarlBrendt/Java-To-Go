package ru.mts.ip.workflow.engine.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;

import java.util.Collection;

@Data
public class IbmmqAuth implements EncryptedDataHandler {

  private String type;
  private IbmmqBasic basic;

  @Data
  public static class IbmmqBasic implements EncryptedDataHandler {
    private String userName;
    private String password;

    @Override
    public void applySecrets(Collection<ClientErrorDescription> errorPouch,
        ExternalProperties resolved) {
      resolved.applySecret(this::getUserName,errorPouch::add,this::setUserName);
    }

    @Override
    public void removeCredentials() {
        userName = null;
        password = null;
    }
  }

  @Override
  public void applySecrets(Collection<ClientErrorDescription> errorPouch,
      ExternalProperties resolved) {
    if (basic != null) {
      basic.applySecrets(errorPouch, resolved);
    }
  }

  @Override
  public void removeCredentials() {
    if (basic != null) {
      basic.removeCredentials();
    }
  }

}
