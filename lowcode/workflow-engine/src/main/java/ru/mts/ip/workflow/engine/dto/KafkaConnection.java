package ru.mts.ip.workflow.engine.dto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

@Data
@Accessors(chain = true)
public class KafkaConnection {

  public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
    if(bootstrapServers != null) {
      secrets.applySecret(this::getBootstrapServers, errorPouch::add, this::setBootstrapServers);
    }
    if(authDef != null) {
      authDef.applySecrets(errorPouch, secrets);
    }
  }

  private String bootstrapServers;
  private KafkaAuth authDef;
  private List<String> tags;
  
  @Data
  @Accessors(chain = true)
  public static class SaslAuth {
    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      secrets.applySecret(this::getUsername, errorPouch::add, this::setUsername);
      secrets.applySecret(this::getPassword, errorPouch::add, this::setPassword);
      if(sslDef != null) {
        sslDef.applySecrets(errorPouch, secrets);
      }
    }
    private Ssl sslDef;
    private String protocol;
    private String mechanism;
    private String username;
    private String password;
    private String tokenUrl;
    
    public void removeCredentials() {
      username = null;
      password = null;
      Optional.ofNullable(sslDef).ifPresent(Ssl::removeCredentials);
    }
  }
  
  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class KafkaAuth {

    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      if(sasl != null) {
        sasl.applySecrets(errorPouch, secrets);
      }
      if(tls != null) {
        tls.applySecrets(errorPouch, secrets);
      }
    }

    private String type;
    private SaslAuth sasl;
    private Tls tls;
    
    public void removeCredentials() {
      Optional.ofNullable(sasl).ifPresent(SaslAuth::removeCredentials);
      Optional.ofNullable(tls).ifPresent(Tls::removeCredentials);
    }
  }
  
  @Data
  @Accessors(chain = true)
  public static class Ssl {
    public Ssl copy() {
      return new Ssl().setTrustStoreType(trustStoreType)
          .setTrustStoreCertificates(trustStoreCertificates);
    }
    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      secrets.applySecret(this::getTrustStoreCertificates, errorPouch::add, this::setTrustStoreCertificates);
    }
    private String trustStoreType;
    private String trustStoreCertificates;
    
    public void removeCredentials() {
      trustStoreCertificates = null;
    }

  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class Tls {
    public Tls copy() {
      return new Tls()
          .setTrustStoreType(trustStoreType)
          .setKeyStoreKey(keyStoreKey)
          .setKeyStoreCertificates(trustStoreCertificates)
          .setTrustStoreCertificates(trustStoreCertificates);
    }
    
    public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties secrets) {
      secrets.applySecret(this::getTrustStoreCertificates, errorPouch::add, this::setTrustStoreCertificates);
      secrets.applySecret(this::getKeyStoreCertificates, errorPouch::add, this::setKeyStoreCertificates);
      secrets.applySecret(this::getKeyStoreKey, errorPouch::add, this::setKeyStoreKey);
    }
    
    private String keyStoreKey;
    private String keyStoreCertificates;
    private String trustStoreType;
    private String trustStoreCertificates;
    
    public void removeCredentials() {
      keyStoreCertificates = null;
      trustStoreCertificates = null;
    }
    
  }

  public void removeCredentials() {
    Optional.ofNullable(authDef).ifPresent(KafkaAuth::removeCredentials);
  }
  
}
