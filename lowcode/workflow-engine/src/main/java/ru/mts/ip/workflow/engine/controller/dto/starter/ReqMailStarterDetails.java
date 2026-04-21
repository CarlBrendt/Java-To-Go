package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ReqMailStarterDetails {
  private ReqMailConnection connectionDef;
  private ReqMailFilter mailFilter;
  private ReqMailPollConfig pollConfig;
  private JsonNode outputTemplate;
  private JsonNode workflowInputValidateSchema;


  @Data
  public static final class ReqMailConnection {
    private String protocol;
    private String host;
    private Integer port;
    private ReqMailAuth mailAuth;
  }

  public record ReqMailFilter(List<String> senders, List<String> subjects,
      OffsetDateTime startMailDateTime) {
  }


  public record ReqMailAuth(String username, String password, ReqMailCertificate certificate) {
  }

  public record ReqMailCertificate(String trustStoreType, String trustStoreCertificates) {
  }

  public record ReqMailPollConfig(long pollDelaySeconds, int maxFetchSize) {
  }
}
