package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqIbmmqStarterDetailsPatch {
  private Optional<String> queueName;
  private Optional<JsonNode> payloadValidateSchema;
  private Optional<JsonNode> outputTemplate;
  private Optional<ReqIbmmqConnectionPatch> connectionDef;

  @Data

  public static final class ReqIbmmqConnectionPatch {
    private Optional<ReqIbmmqAuthPatch> authDef;
    private Optional<String> addresses;
    private Optional<Integer> ccsid;
    private Optional<String> queueManager;
    private Optional<String> channel;
  }

  @Data

  public static final class ReqIbmmqAuthPatch {
    private Optional<String> type;
    private Optional<ReqIbmmqBasicPatch> basic;
  }

  @Data
  public static final class ReqIbmmqBasicPatch {
    private Optional<String> userName;
    private Optional<String> password;
  }
}
