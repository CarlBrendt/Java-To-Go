package ru.mts.workflowmail.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ReqCompatibilityStarter {
  private ReqMailConsumerForInternal consumer;
  private JsonNode workflowInputValidateSchema;
}
