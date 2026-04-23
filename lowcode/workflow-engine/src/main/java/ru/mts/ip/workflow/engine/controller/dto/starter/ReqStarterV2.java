package ru.mts.ip.workflow.engine.controller.dto.starter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

import static ru.mts.ip.workflow.engine.Const.StarterType.KAFKA_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.MAIL_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.RABBITMQ_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.REST_CALL;
import static ru.mts.ip.workflow.engine.Const.StarterType.SAP_INBOUND;

@Data
public class ReqStarterV2 {
  @Schema(allowableValues = {REST_CALL, SAP_INBOUND, KAFKA_CONSUMER, RABBITMQ_CONSUMER, MAIL_CONSUMER},
      example = REST_CALL, requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  private String type;
  @NotNull
  private String name;
  private String description;
  @Schema(description = "Дата и время запуска стартера",
      example = "2023-10-01T12:00:00+03:00", type = "string", format = "date-time")
  private OffsetDateTime startDateTime;
  @Schema(description = "Дата и время завершения работы стартера",
      example = "2023-10-01T12:00:00+03:00", type = "string", format = "date-time")
  private OffsetDateTime endDateTime;
  @NotNull
  private UUID workflowDefinitionToStartId;
  private ReqSapStarterDetails sapInbound;
  private ReqKafkaStarterDetails kafkaConsumer;
  private ReqRabbitmqStarterDetails rabbitmqConsumer;
  private ReqSchedulerStarterDetails scheduler;
  private ReqMailStarterDetails mailConsumer;
  private ReqIbmmqStarterDetails ibmmqConsumer;
}
