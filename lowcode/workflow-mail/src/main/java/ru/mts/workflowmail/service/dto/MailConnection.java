package ru.mts.workflowmail.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailConnection {
  private String protocol;
  private String host;
  private Integer port;
  private MailAuth mailAuth;

  @Data
  public static final class MailAuth {
    private String username;
    private String password;
    private MailCertificate certificate;
  }

  @Data
  public static final class MailCertificate {
    private String trustStoreType;
    private String trustStoreCertificates;
  }
}
