package ru.mts.ip.workflow.engine.temporal;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowHistory;

@Data
@NoArgsConstructor
public class ExecutionContext {
  
  private EngineConfigurationProperties engineConfig;
  private DetailedWorkflowDefinition workflowDefinition;
  private WorkflowHistory hist;
  private ExternalProperties externalProperties;
  private Map<String, String> secretVariables;
  //private Map<String, String> mdc;
  private Variables generatedVariables;
  private ExecutionContext parent;
  
  @JsonIgnore
  public Variables compileResultVariableContext() {
    return hist.compileResultVariableContext();
  }

  @JsonIgnore
  public Variables compileFullDetailedVariableContext() {
    return hist.compileFullDetailedVariableContext();
  }

  @JsonIgnore
  public Variables compileDetailedVariableContext() {
    return hist.compileDetailedVariableContext();
  }

  @JsonIgnore
  public Map<String, JsonNode> getWorkflowInitVariables(){
    return parent == null ? hist.getInitVariables().getVars() : parent.getWorkflowInitVariables();
  }

  @JsonIgnore
  public Map<String, String> getCompiledVariableSecrets() {
    Map<String, String> result = new HashMap<>();
    if(externalProperties != null && secretVariables != null) {
      secretVariables.forEach((k,v) -> {
        result.put(k, externalProperties.get(v));
      });
    }
    return result;
  }
  

  public ExecutionContext(ExecutionContext parent, String aggregateActivityId, Variables initVariables) {
    this.parent = parent;
    this.engineConfig = parent.getEngineConfig();
    this.workflowDefinition = parent.getWorkflowDefinition();
    this.hist = new WorkflowHistory(initVariables, aggregateActivityId);
    this.externalProperties = parent.getExternalProperties();
    this.secretVariables = parent.getSecretVariables();
    this.generatedVariables = parent.getGeneratedVariables();
    //this.mdc = parent.getMdc();
  }

  public ExecutionContext(EngineConfigurationProperties engineConfig,
      DetailedWorkflowDefinition workflowDefinition, ExternalProperties externalProperties, Map<String, String> secretVariables, Variables initVariables) {
    this.engineConfig = engineConfig;
    this.workflowDefinition = workflowDefinition;
    this.hist = new WorkflowHistory(initVariables, null);
    this.externalProperties = externalProperties;
    this.secretVariables = secretVariables;
    //this.mdc = mdc;
  }

  public ExecutionContext(DetailedWorkflowDefinition workflowDefinition,
      Variables initVariables, Variables generatedVariables) {
    this.workflowDefinition = workflowDefinition;
    this.hist = new WorkflowHistory(initVariables, null);
    this.generatedVariables = generatedVariables;
  }


}
