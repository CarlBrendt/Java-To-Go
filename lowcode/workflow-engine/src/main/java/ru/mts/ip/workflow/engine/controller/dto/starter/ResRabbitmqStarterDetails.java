package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ResRabbitmqStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
  private String queue;
  private ResRabbitmqConnection connectionDef;

  @Data
  @Accessors(chain = true)
  public static class ResRabbitmqConnection {
    private String userName;
    private String userPass;
    private List<String> addresses;
    private String virtualHost;
  }
}
