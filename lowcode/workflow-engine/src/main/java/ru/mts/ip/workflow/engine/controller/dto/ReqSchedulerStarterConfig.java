package ru.mts.ip.workflow.engine.controller.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.service.scheduler.SchedulerStarter;

import java.time.OffsetDateTime;

@Data
public class ReqSchedulerStarterConfig {
  private String type;
  private SchedulerStarter.Cron cron;
  private SchedulerStarter.SimpleDuration simple;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
}
