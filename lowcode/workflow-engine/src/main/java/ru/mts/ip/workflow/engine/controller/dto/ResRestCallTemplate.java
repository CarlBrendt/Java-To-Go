package ru.mts.ip.workflow.engine.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ResRestCallTemplate {

  private UUID id;
  private String name;
  private String tenantId;
  private String environment;
  private LocalDateTime createTime;
  private Integer version;
  private ResRestCallDetails details;

  @Data
  public static class ResRestCallDetails {
    @NotEmpty
    public String method;
    @NotEmpty
    public String url;
    public JsonNode bodyTemplate;
  }
}
