package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

@Data
public class ReqRabbitmqConsumerStarter {
  @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 1, example = "queue")
  private String queue;
  @Schema(requiredMode = RequiredMode.REQUIRED)
  private ReqCreateExecutableWorkflowDefinition.ReqRabbitmqConnectionDef connectionDef;
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
}
