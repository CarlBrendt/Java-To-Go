package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.entity.KafkaStarterDetails;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class StarterDto {
  private UUID id;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  private OffsetDateTime createTime;
  private OffsetDateTime changeTime;
  private UUID workflowDefinitionToStartId;
  private KafkaStarterDetails kafkaConsumer;
}
