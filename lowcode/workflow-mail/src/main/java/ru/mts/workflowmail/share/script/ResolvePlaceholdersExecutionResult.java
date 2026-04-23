package ru.mts.workflowmail.share.script;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResolvePlaceholdersExecutionResult {
  private JsonNode resultNode;
}
