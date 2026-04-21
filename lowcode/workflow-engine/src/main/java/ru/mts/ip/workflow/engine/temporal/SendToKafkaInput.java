package ru.mts.ip.workflow.engine.temporal;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.Accessors;


public record SendToKafkaInput(String activityId, KafkaConnection connectionDef,
    KafkaMessage message, String topic, String key, Map<String, String> headers) {

  public record KafkaMessage(JsonNode payload) {
  }


  @Accessors(chain = true)
  public record KafkaConnection(String bootstrapServers, KafkaAuth authDef, List<String> tags) {
  }


  public record KafkaAuth(String type, SaslAuth sasl, Tls tls) {
  }


  public record SaslAuth(Ssl sslDef, String protocol, String mechanism, String username,
      String password, String tokenUrl) {
  }


  public record Ssl(String trustStoreType, String trustStoreCertificates) {

  }
  
  public record Tls(String keyStoreKey, String keyStoreCertificates, String trustStoreType, String trustStoreCertificates) {
    
  }

}
