package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
public class ResIbmmqStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode outputTemplate;
  private String queueName;
  private ResIbmmqConnection connectionDef;

  @Data
  @Accessors(chain = true)
  public static class ResIbmmqConnection {
    private ResIbmmqAuth authDef;
    private String addresses;
    private Integer ccsid;
    private String queueManager;
    private String channel;
  }

  @Data
  public static class ResIbmmqAuth {

    private String type;
    private ResIbmmqBasic basic;

    @Data
    public static class ResIbmmqBasic {
      private String userName;
      private String password;
    }

  }
}
