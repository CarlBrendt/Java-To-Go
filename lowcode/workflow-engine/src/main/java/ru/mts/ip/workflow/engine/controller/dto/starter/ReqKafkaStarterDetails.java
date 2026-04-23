package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.KafkaConnection;

@Data
@Accessors(chain = true)
public class ReqKafkaStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode keyValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
  private JsonNode workflowInputValidateSchema;
  private String topic;
  private KafkaConnection connectionDef;
  private String consumerGroupId;
}
