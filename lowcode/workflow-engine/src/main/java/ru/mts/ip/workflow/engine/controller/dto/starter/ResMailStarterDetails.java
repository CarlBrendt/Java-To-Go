package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ResMailStarterDetails {
  private ResMailConnection connectionDef;
  private ResMailFilter mailFilter;
  private ResMailPollConfig pollConfig;
  private JsonNode outputTemplate;


  @Data
  public static final class ResMailConnection {
    private String protocol;
    private String host;
    private Integer port;
    private ResMailAuth mailAuth;
  }

  public record ResMailFilter(List<String> senders, List<String> subjects,
      OffsetDateTime startMailDateTime) {
  }


  public record ResMailAuth(String username, String password, ResMailCertificate certificate) {
  }

  @Data
  public static final class ResMailCertificate {
    private String trustStoreType;
    private String trustStoreCertificates;
  }

  public record ResMailPollConfig(long pollDelaySeconds, int maxFetchSize) {
  }
}
