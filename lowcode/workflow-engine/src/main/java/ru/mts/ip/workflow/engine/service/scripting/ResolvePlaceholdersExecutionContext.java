package ru.mts.ip.workflow.engine.service.scripting;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;

@Data
@Accessors(chain = true)
public class ResolvePlaceholdersExecutionContext {
  private ScriptExecutionContext scriptContext;
  private JsonNode node;
}
