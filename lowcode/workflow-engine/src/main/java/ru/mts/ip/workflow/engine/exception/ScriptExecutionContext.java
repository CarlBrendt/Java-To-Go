package ru.mts.ip.workflow.engine.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.service.Variables;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ScriptExecutionContext {

  private JsonNode vars;
  private ScriptWorkflowView wf;
  
  @Data
  public static class ScriptWorkflowView{
    private String businessKey;
    private Instant workflowExpiration;
    private Map<String, String> secrets;
    private Map<String, JsonNode> initVariables;
    private Map<String, List<JsonNode>> consumedMessages;
  }

  public ScriptExecutionContext(Variables vars, ScriptWorkflowView wf) {
    this.vars = vars.asNode();
    this.wf = wf;
  }

  public ScriptExecutionContext(Variables vars) {
    this(vars.asNode(), new ScriptWorkflowView());
  }

  public ScriptExecutionContext(JsonNode node) {
    this(node, null);
  }

  public ScriptExecutionContext(JsonNode vars, ScriptWorkflowView wf) {
    this.vars = vars;
    this.wf = wf;
  }
  
}
