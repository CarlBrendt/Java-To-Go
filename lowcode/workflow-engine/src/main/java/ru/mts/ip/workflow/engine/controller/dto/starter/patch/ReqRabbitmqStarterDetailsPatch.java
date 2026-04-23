package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqRabbitmqStarterDetailsPatch {
  private Optional<JsonNode> payloadValidateSchema;
  private Optional<JsonNode> headersValidateSchema;
  private Optional<JsonNode> outputTemplate;
  private Optional<String> queue;
  private Optional<ReqRabbitmqConnectionPatch> connectionDef;

  @Data
  public static final class ReqRabbitmqConnectionPatch {
    private Optional<String> userName;
    private Optional<String> userPass;
    private Optional<List<String>> addresses;
    private Optional<String> virtualHost;
  }
}
