package ru.mts.ip.workflow.engine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;

import java.util.Collection;
import java.util.Optional;

@Data
public class IbmmqConsumer implements EncryptedDataHandler {
  private IbmmqConnection connectionDef;
  private String queueName;
  private JsonNode payloadValidateSchema;
  private JsonNode outputTemplate;

  @Override
  public void applySecrets(Collection<ClientErrorDescription> errorPouch,
      ExternalProperties resolved) {
    if (resolved != null && connectionDef != null) {
      connectionDef.applySecrets(errorPouch, resolved);
    }
  }

  @Override
  public void removeCredentials() {
    Optional.ofNullable(connectionDef).ifPresent(IbmmqConnection::removeCredentials);
  }
}
