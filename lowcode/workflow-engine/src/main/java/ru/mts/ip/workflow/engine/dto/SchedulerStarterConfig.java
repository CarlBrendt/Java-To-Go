package ru.mts.ip.workflow.engine.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.service.scheduler.SchedulerStarter;

import java.time.OffsetDateTime;

@Data
public class SchedulerStarterConfig {
  private SchedulerStarter.Cron cron;
  private String type;
  private SchedulerStarter.SimpleDuration simple;
}
