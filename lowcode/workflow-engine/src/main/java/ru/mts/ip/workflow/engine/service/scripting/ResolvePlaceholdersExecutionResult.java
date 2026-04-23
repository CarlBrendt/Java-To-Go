package ru.mts.ip.workflow.engine.service.scripting;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResolvePlaceholdersExecutionResult {
  private JsonNode resultNode;
}
