package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class ReqKafkaConsumerStarter {
  private String topic;
  private String consumerGroupId;
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode keyValidateSchema;
  private JsonNode outputTemplate;
  private JsonNode workflowInputValidateSchema;
  private ReqCreateExecutableWorkflowDefinition.ReqKafkaConnection connectionDef;
}
