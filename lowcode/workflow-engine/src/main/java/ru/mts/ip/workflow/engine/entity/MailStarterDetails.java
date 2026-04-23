package ru.mts.ip.workflow.engine.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MailStarterDetails {
  private DetailsMailConnection connectionDef;
  private DetailsMailFilter mailFilter;
  private MailPollConfig pollConfig;
  private JsonNode outputTemplate;

  @Data
  public static final class DetailsMailConnection {
    private String protocol;
    private String host;
    private Integer port;
    private DetailsMailAuth mailAuth;
  }

  public record DetailsMailFilter(List<String> senders, List<String> subjects,
      OffsetDateTime startMailDateTime) {
  }

  @Data
  public static final class DetailsMailAuth {
    private String username;
    private String password;
    private DetailsMailCertificate certificate;
  }

  @Data
  public static final class DetailsMailCertificate {
    private String trustStoreType;
    private String trustStoreCertificates;
  }

  @Data
  public static class MailPollConfig {
    private long pollDelaySeconds;
    private int maxFetchSize;
  }
}
