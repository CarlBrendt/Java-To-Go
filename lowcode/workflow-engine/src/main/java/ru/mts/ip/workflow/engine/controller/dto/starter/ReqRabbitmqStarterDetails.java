package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ReqRabbitmqStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
  private JsonNode workflowInputValidateSchema;
  private String queue;
  private ReqRabbitmqConnection connectionDef;

  @Data
  @Accessors(chain = true)
  public static class ReqRabbitmqConnection {
    private String userName;
    private String userPass;
    private List<String> addresses;
    private String virtualHost;
  }
}
