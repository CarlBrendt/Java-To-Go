package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqKafkaStarterDetailsPatch {
  private Optional<JsonNode> payloadValidateSchema;
  private Optional<JsonNode> keyValidateSchema;
  private Optional<JsonNode> headersValidateSchema;
  private Optional<JsonNode> outputTemplate;
  private Optional<String> topic;
  private Optional<String> consumerGroupId;
  private Optional<ReqKafkaConnectionPatch> connectionDef;

  @Data
  public static final class ReqKafkaConnectionPatch {
    private Optional<String> bootstrapServers;
    private Optional<ReqKafkaAuthPatch> authDef;
    private Optional<List<String>> tags;
  }

  @Data
  public static final class ReqKafkaAuthPatch {
    private Optional<String> type;
    private Optional<ReqSaslAuthPatch> sasl;
    private Optional<ReqTlsPatch> tls;
  }

  @Data
  public static final class ReqSaslAuthPatch {
    private Optional<ReqSslPatch> sslDef;
    private Optional<String> protocol;
    private Optional<String> mechanism;
    private Optional<String> username;
    private Optional<String> password;
    private Optional<String> tokenUrl;
  }

  @Data
  public static final class ReqSslPatch {
    private Optional<String> trustStoreType;
    private Optional<String> trustStoreCertificates;
  }

  @Data
  public static final class ReqTlsPatch {
    private Optional<String> keyStoreKey;
    private Optional<String> keyStoreCertificates;
    private Optional<String> trustStoreType;
    private Optional<String> trustStoreCertificates;
  }
}
