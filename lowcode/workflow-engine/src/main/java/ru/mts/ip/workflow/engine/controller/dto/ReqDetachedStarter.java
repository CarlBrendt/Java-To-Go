package ru.mts.ip.workflow.engine.controller.dto;

import static ru.mts.ip.workflow.engine.Const.StarterType.KAFKA_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.MAIL_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.RABBITMQ_CONSUMER;
import static ru.mts.ip.workflow.engine.Const.StarterType.REST_CALL;
import static ru.mts.ip.workflow.engine.Const.StarterType.SAP_INBOUND;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqIbmmqStarterDetails;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqSapStarterDetails;

@Data
public class ReqDetachedStarter {
  private List<String> tags;
  @Schema(allowableValues = {REST_CALL, SAP_INBOUND, KAFKA_CONSUMER, RABBITMQ_CONSUMER, MAIL_CONSUMER},
      example = REST_CALL, requiredMode = Schema.RequiredMode.REQUIRED)
  private String type;
  private String name;
  private String description;
  @Schema(description = "Дата и время запуска стартера",
      example = "2023-10-01T12:00:00+03:00", type = "string", format = "date-time")
  private OffsetDateTime startDateTime;
  @Schema(description = "Дата и время завершения работы стартера",
      example = "2023-10-01T12:00:00+03:00", type = "string", format = "date-time")
  private OffsetDateTime endDateTime;
  private UUID workflowDefinitionToStartId;
  private ReqSapStarterDetails sapInbound;
  private ReqKafkaConsumerStarter kafkaConsumer;
  private ReqRabbitmqConsumerStarter rabbitmqConsumer;
  private ReqSchedulerStarterConfig scheduler;
  private ReqMailConsumerStarter mailConsumer;
  private ReqIbmmqStarterDetails ibmmqConsumer;

}
