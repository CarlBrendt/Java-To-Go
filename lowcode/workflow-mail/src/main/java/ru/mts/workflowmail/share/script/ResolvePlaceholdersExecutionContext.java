package ru.mts.workflowmail.share.script;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ResolvePlaceholdersExecutionContext {
  private ScriptExecutionContext scriptContext;
  private JsonNode node;
}
