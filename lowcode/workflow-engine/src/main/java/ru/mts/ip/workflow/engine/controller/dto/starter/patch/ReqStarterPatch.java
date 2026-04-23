package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqStarterPatch {
  private Optional<OffsetDateTime> startDateTime;
  private Optional<OffsetDateTime> endDateTime;
  private Optional<ReqMailStarterDetailsPatch> mailConsumer;
  private Optional<ReqKafkaStarterDetailsPatch> kafkaConsumer;
  private Optional<ReqRabbitmqStarterDetailsPatch> rabbitmqConsumer;
  private Optional<ReqIbmmqStarterDetailsPatch> ibmmqConsumer;
  private Optional<ReqSapStarterDetailsPatch> sapInbound;
  private Optional<ReqSchedulerStarterDetailsPatch> scheduler;
}
