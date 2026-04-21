package ru.mts.ip.workflow.engine.dto;

import java.util.Collection;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.EncryptedDataHandler;

@Data
public class KafkaConsumer implements EncryptedDataHandler {
  private KafkaConnection connectionDef;
  private String topic;
  private String consumerGroupId;
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode keyValidateSchema;
  private JsonNode outputTemplate;

  @Override
  public void applySecrets(Collection<ClientErrorDescription> errorPouch, ExternalProperties resolved) {
    if(resolved != null && connectionDef != null) {
      connectionDef.applySecrets(errorPouch, resolved);
    }
  }

  @Override
  public void removeCredentials() {
    Optional.ofNullable(connectionDef).ifPresent(KafkaConnection::removeCredentials);
  }
}
