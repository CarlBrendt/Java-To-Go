package ru.mts.ip.workflow.engine.dto;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext.ScriptWorkflowView;

@Data
@Accessors(chain = true)
public class ScriptErrorContext {
  private JsonNode variableContext;
  private ScriptWorkflowView wf;
  private String rejectedScript;
  private String systemMessage;
  private List<String> unknownVariables;
}
