package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.KafkaConnection;

@Data
@Accessors(chain = true)
public class ResKafkaStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode keyValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
  private String topic;
  private String consumerGroupId;
  private KafkaConnection connectionDef;
}
