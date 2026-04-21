package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Duration;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqSchedulerStarterDetailsPatch {
  private Optional<String> type;
  private Optional<ReqCronPatch> cron;
  private Optional<ReqSimpleDurationPatch> simple;

  @Data
  public static final class ReqCronPatch {
    private Optional<String> dayOfWeek;
    private Optional<String> month;
    private Optional<String> dayOfMonth;
    private Optional<String> hour;
    private Optional<String> minute;
  }

  @Data
  public static final class ReqSimpleDurationPatch {
    private Optional<Duration> duration;
  }
}
