package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReqMailConsumerStarter {
  private ReqMailConnection connectionDef;
  private ReqMailFilter mailFilter;
  private ReqMailPollConfig pollConfig;
  private JsonNode outputTemplate;


  public record ReqMailConnection(
      //TODO schema available values
      String protocol, String host, Integer port, ReqMailAuth mailAuth) {
  }


  public record ReqMailAuth(String username, String password, ReqMailCertificate certificate) {
  }

  public record ReqMailCertificate(String trustStoreType, String trustStoreCertificates) {
  }


  public record ReqMailFilter(
      List<String> senders,
      List<String> subjects,
      @Schema(description = "Дата и время писем с учетом смещения", example = "2023-10-01T12:00:00+03:00", type = "string", format = "date-time")
      OffsetDateTime startMailDateTime) {
  }

  public record ReqMailPollConfig(long pollDelaySeconds, int maxFetchSize) {
  }

}
