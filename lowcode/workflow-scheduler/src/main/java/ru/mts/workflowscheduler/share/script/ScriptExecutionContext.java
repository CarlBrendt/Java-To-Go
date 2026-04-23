package ru.mts.workflowscheduler.share.script;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.service.Variables;

import java.util.Map;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ScriptExecutionContext {

  private JsonNode vars;
  private ScriptWorkflowView wf;

  @Data
  public static class ScriptWorkflowView {
    private String businessKey;
    private Map<String, String> secrets;
    private Map<String, JsonNode> initVariables;
  }

  public ScriptExecutionContext(Variables vars, ScriptWorkflowView wf) {
    this.vars = vars.asNode();
    this.wf = wf;
  }

  public ScriptExecutionContext(Variables vars) {
    this.vars = vars.asNode();
    this.wf = new ScriptWorkflowView();
  }

  public ScriptExecutionContext(JsonNode vars, ScriptWorkflowView wf) {
    this.vars = vars;
    this.wf = wf;
  }

  public ScriptExecutionContext(JsonNode node) {
    this(node, null);
  }

}
