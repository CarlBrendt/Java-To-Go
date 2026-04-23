package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class ResStarter {
  private UUID id;
  private String name;
  private String type;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  private OffsetDateTime createTime;
  private OffsetDateTime changeTime;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private UUID workflowDefinitionToStartId;
  private JsonNode workflowInputValidateSchema;
  private ResKafkaStarterDetails kafkaConsumer;
  private ResRabbitmqStarterDetails rabbitmqConsumer;
  private ResSapStarterDetails sapInbound;
  private ResMailStarterDetails mailConsumer;
  private ResSchedulerStarterDetails scheduler;
  private ResIbmmqStarterDetails ibmmqConsumer;
  private ResInnerWorker worker;

  @Data
  @Accessors(chain = true)
  public static class ResInnerWorker {
    private UUID id;
    private OffsetDateTime createTime;
    private OffsetDateTime changeTime;
    private UUID executorId;
    private OffsetDateTime lockedUntilTime;
    private OffsetDateTime overdueTime;
    private Integer retryCount;
    private String status;
    private String errorMessage;
    private String errorStackTrace;
  }
}

