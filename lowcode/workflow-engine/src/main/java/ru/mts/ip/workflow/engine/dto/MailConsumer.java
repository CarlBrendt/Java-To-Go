package ru.mts.ip.workflow.engine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Data
public class MailConsumer implements EncryptedDataHandler {
  private MailConnection connectionDef;
  private MailFilter mailFilter;
  private MailPollConfig pollConfig;
  private JsonNode outputTemplate;

  public void applySecrets(
      Collection<ClientErrorDescription> errorPouch, ExternalProperties resolved) {
    if(resolved != null && connectionDef != null) {
      connectionDef.applySecrets(errorPouch, resolved);
    }
  }
  
  public void removeCredentials() {
    Optional.ofNullable(connectionDef).ifPresent(MailConnection::removeCredentials);
  }


  @Data
  public static final class MailConnection {
    private String protocol;
    private String host;
    private Integer port;
    private MailAuth mailAuth;

    public void removeCredentials() {
      Optional.ofNullable(mailAuth).ifPresent(MailAuth::removeCredentials);
    }

    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      if (host != null) {
        secrets.applySecret(this::getHost, errorPouch::add, this::setHost);
      }
      if (mailAuth != null) {
        mailAuth.applySecrets(errorPouch, secrets);
      }
    }
  }


  @Data
  public static final class MailAuth {
    private String username;
    private String password;
    private MailCertificate certificate;

    public void removeCredentials() {
      username = null;
      password = null;
      if (certificate != null){
        certificate.removeCredentials();
      }
    }


    public void applySecrets(Collection<ClientErrorDescription> errorPouch,
        ExternalProperties secrets) {
      if (password != null) {
        secrets.applySecret(this::getPassword, errorPouch::add, this::setPassword);
      }
      if (certificate != null) {
        certificate.applySecrets(errorPouch, secrets);
      }
    }
  }

  @Data
  public static final class MailCertificate {
    private String trustStoreType;
    private String trustStoreCertificates;

    public void removeCredentials() {
      trustStoreCertificates = null;
    }

    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      if (trustStoreCertificates != null) {
        secrets.applySecret(this::getTrustStoreCertificates, errorPouch::add, this::setTrustStoreCertificates);
      }
    }
  }


  public record MailFilter(List<String> senders, List<String> subjects,
      OffsetDateTime startMailDateTime) {
  }

    public record MailPollConfig(long pollDelaySeconds, int maxFetchSize) {
  }


}
