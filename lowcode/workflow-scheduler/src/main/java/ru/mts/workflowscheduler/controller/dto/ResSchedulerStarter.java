package ru.mts.workflowscheduler.controller.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ResSchedulerStarter {
  private String name;
  private String tenantId;
  private String description;
  private UUID workflowDefinitionToStartId;
  private String type;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private ResCron cron;
  private ResSimpleDuration simple;

  @Data
  public static class ResSimpleDuration {
    String duration;
  }

  @Data
  public static class ResCron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }
}
