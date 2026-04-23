package ru.mts.ip.workflow.engine.controller.dto.starter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;

@Data
@Accessors(chain = true)
public class ResSchedulerStarterDetails {
  private String type;
  private ResCron cron;
  private ResSimpleDuration simple;


  @Data
  public static class ResCron {
    private String dayOfWeek;
    private String month;
    private String dayOfMonth;
    private String hour;
    private String minute;
  }


  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResSimpleDuration {
    private Duration duration;
  }
}
