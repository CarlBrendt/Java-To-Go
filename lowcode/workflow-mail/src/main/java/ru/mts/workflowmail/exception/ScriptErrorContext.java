package ru.mts.workflowmail.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowmail.share.script.ScriptExecutionContext;

import java.util.List;

@Data
@Accessors(chain = true)
public class ScriptErrorContext {
  private JsonNode variableContext;
  private ScriptExecutionContext.ScriptWorkflowView wf;
  private String rejectedScript;
  private String systemMessage;
  private List<String> unknownVariables;
}
