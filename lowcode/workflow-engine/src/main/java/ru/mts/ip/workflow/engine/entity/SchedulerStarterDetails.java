package ru.mts.ip.workflow.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
public class SchedulerStarterDetails {
  private String type;
  private Cron cron;
  private SimpleDuration simple;


  @Data
  public static class Cron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }


  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleDuration {
    private Duration duration;
  }
}
