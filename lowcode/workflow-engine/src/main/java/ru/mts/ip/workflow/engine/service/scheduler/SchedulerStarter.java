package ru.mts.ip.workflow.engine.service.scheduler;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class SchedulerStarter {
  private String name;
  private String tenantId;
  private String description;
  private UUID workflowDefinitionToStartId;
  private String type;
  private Cron cron;
  private SimpleDuration simple;

  @Data
  public static class SimpleDuration {
    private String duration;
  }

  @Data
  public static class Cron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }
}
