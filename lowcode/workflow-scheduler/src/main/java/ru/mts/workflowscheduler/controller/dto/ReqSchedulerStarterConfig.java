package ru.mts.workflowscheduler.controller.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ReqSchedulerStarterConfig {
  private String type;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private ReqSchedulerStarter.ReqCron cron;
  private ReqSchedulerStarter.ReqSimpleDuration simple;
}
