package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ReqIbmmqStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode outputTemplate;
  private String queueName;
  private ReqIbmmqConnection connectionDef;

  @Data
  @Accessors(chain = true)
  public static class ReqIbmmqConnection {
    private ReqIbmmqAuth authDef;
    private String addresses;
    private Integer ccsid;
    private String queueManager;
    private String channel;
  }

  @Data
  @Accessors(chain = true)
  public static class ReqIbmmqAuth {
    private String type;
    private ReqIbmmqBasic basic;
  }

  @Data
  @Accessors(chain = true)
  public static class ReqIbmmqBasic {
    private String userName;
    private String password;
  }
}
