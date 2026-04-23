package ru.mts.ip.workflow.engine.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.utility.DateHelper;

@Slf4j
@UtilityClass
public class SerializeUtils {

  private static final ObjectMapper OM = new ObjectMapper();
  private static final Base64.Encoder NEXT_PAGE_TOKEN_ENCODER = Base64.getEncoder();
  private static final Base64.Decoder NEXT_PAGE_TOKEN_DECODER = Base64.getDecoder();
  
  public Optional<JsonNode> asJsonNode(byte[] bytes) {
    try {
      return Optional.of(OM.readTree(bytes));
    } catch (IOException ex) {
      log.error("SerializeUtils::asJsonNode", ex);
    }
    return Optional.empty();
  }

  public Optional<String> jsonTextToString(byte[] bytes) {
    return asJsonNode(bytes).filter(JsonNode::isTextual).map(JsonNode::asText);
  }

  public String encodeNextPageToken(byte[] bytes) {
    return NEXT_PAGE_TOKEN_ENCODER.encodeToString(bytes);
  }

  public byte[] decodeNextPageToken(String token) {
    return NEXT_PAGE_TOKEN_DECODER.decode(token);
  }

  public boolean validNextPageToken(String text) {
    try {
      NextPageToken token = OM.readValue(decodeNextPageToken(text), NextPageToken.class);
      if(DateHelper.testISOValidDate(token.getCloseTime()) && DateHelper.testISOValidDate(token.getStartTime())) {
        UUID.fromString(token.getRunID());
        return true;
      }
    } catch (Exception ignore) {}
    return false;
  }
  
  @Data
  static class NextPageToken {
    @JsonProperty("CloseTime")
    private String closeTime;
    @JsonProperty("StartTime")
    private String startTime;
    @JsonProperty("RunID")
    private String runID;
  }


  public Optional<Long> jsonNumber(byte[] bytes) {
    return asJsonNode(bytes).filter(JsonNode::isNumber).map(JsonNode::asLong);
  }
  
}
