package ru.mts.workflowmail.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CompatibilityStarter {
  private MailConsumerForInternal consumer;
  private JsonNode workflowInputValidateSchema;
}
