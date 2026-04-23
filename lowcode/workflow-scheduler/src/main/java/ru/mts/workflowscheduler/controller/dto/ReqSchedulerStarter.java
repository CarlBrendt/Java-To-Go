package ru.mts.workflowscheduler.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ReqSchedulerStarter {
  private String name;
  private String tenantId;
  private String description;
  private UUID workflowDefinitionToStartId;
  private String type;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private ReqCron cron;
  private ReqSimpleDuration simple;

  @Data
  public static class ReqSimpleDuration {
    @Schema(type = "string", format = "ISO_8601_duration", example = "PT100S")
    Duration duration;
  }

  @Data
  public static class ReqCron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }
}
