package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.service.Const;

import java.time.Duration;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Starter {
  private UUID id;
  private String name;
  private final String type = Const.StarterType.SCHEDULER;
  private String tenantId;
  private String description;
  private UUID workflowDefinitionToStartId;
  private JsonNode workflowInputValidateSchema;
  private SchedulerStarterDetails scheduler;


  @Data
  @Accessors(chain = true)
  public static class SchedulerStarterDetails {
    private String type;
    private SimpleDuration simple;
    private Cron cron;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleDuration {
    private Duration duration;
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
