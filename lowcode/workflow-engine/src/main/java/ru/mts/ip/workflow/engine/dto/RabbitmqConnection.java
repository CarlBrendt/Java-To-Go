package ru.mts.ip.workflow.engine.dto;

import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

@Data
@Accessors(chain = true)
public class RabbitmqConnection {

  private String userName;
  private String userPass;
  private List<String> addresses;
  private String virtualHost;


  public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
    if(virtualHost != null) {
      secrets.applySecret(this::getVirtualHost, errorPouch::add, this::setVirtualHost);
    }
    if(userName != null) {
      secrets.applySecret(this::getUserName,  errorPouch::add, this::setUserName);
    }
    if(userPass != null) {
      secrets.applySecret(this::getUserPass, errorPouch::add, this::setUserPass);
    }
    if(addresses != null) {
      for (int i = 0; i < addresses.size(); i++) {
        int index = i;
        String address = addresses.get(index);
        if (address != null) {
          secrets.applySecret(() -> addresses.get(index), errorPouch::add, updAddress -> {
            this.addresses.set(index, updAddress);
          });
        }
      }
    }
  }
  
  public void removeCredentials() {
    userName = null;
    userPass = null;
  }
  
}
