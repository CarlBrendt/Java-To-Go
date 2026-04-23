package ru.mts.workflowscheduler.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CompatibilityStarter {
  private Void consumer;//TODO Void заменить на реальный объект
  private JsonNode workflowInputValidateSchema;
}
