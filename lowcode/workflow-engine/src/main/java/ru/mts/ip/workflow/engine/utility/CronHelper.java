package ru.mts.ip.workflow.engine.utility;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.scheduling.support.CronExpression;
import ru.mts.ip.workflow.engine.entity.SchedulerStarterDetails;

import java.util.Optional;

@UtilityClass
public class CronHelper {

  @SneakyThrows
  public static CronExpression getCronExpression(SchedulerStarterDetails.Cron cron) {
    String seconds = "0";
    String cronExpression = String.format("%s %s %s %s %s %s",
        seconds,
        Optional.ofNullable(cron.getMinute()).orElse("*"),
        Optional.ofNullable(cron.getHour()).orElse("*"),
        Optional.ofNullable(cron.getDayOfMonth()).orElse("*"),
        Optional.ofNullable(cron.getMonth()).orElse("*"),
        Optional.ofNullable(cron.getDayOfWeek()).orElse("*"));
    return CronExpression.parse(cronExpression);
  }
}
