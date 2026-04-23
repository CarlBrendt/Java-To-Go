package ru.mts.ip.workflow.engine.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ClientError;

@Data
@Accessors(chain = true)
public class WorkflowHistory {

  private Map<String, ExecutionStat> hist = new LinkedHashMap<>();
  private final Variables initVariables;
  private final Variables continuedVariables;
  private final String aggregateActivityId;
  
  public WorkflowHistory(@NonNull Variables initVariables) {
    this(initVariables, null);
  }

  public WorkflowHistory(Variables initVariables, String aggregateActivityId) {
    this(initVariables, new Variables(), aggregateActivityId);
  }

  public WorkflowHistory(Variables initVariables, Variables continuedVariables,  String aggregateActivityId) {
    this.initVariables = initVariables;
    this.aggregateActivityId = aggregateActivityId;
    this.continuedVariables = continuedVariables;
  }

  public WorkflowHistory() {
    this(new Variables());
  }
  
  public static enum ActivityExecutionStatus {
    IN_PROGRESS, COMPLETED, ERROR, CANCELED, STOPPED, IGNORED;
  }
  
  public Variables compileResultVariableContext() {
    return compileMergedVariableContext(Variables::new);
  }

  public Variables compileFullDetailedVariableContext() {
    Variables merged = compileMergedVariableContext(initVariables::copy);
    merged.putAll(asVariable());
    return merged;
  }

  public Variables compileDetailedVariableContext() {
    return compileMergedVariableContext(initVariables::copy);
  }
  
  private Variables asVariable() {
    var res = new Variables();
    hist.forEach((k,v) -> {
      var activityOutput = v.getFilteredOutput();
      if(activityOutput != null) {
        res.put(k, activityOutput.asNode());
      }
    });
    return res;
  }

  public Variables compileMergedVariableContext(Supplier<Variables> initVarsSupplier) {
    Variables res = initVarsSupplier.get();
    hist.forEach((k,v) -> {
      var activityOutput = v.getFilteredOutput();
      if(activityOutput != null) {
        res.putAll(activityOutput);
      }
    });
    return res;
  }

  @Data
  @Accessors(chain = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ExecutionStat {
    private String activityid;
    private String aggregateActivityId;
    @JsonIgnore
    private List<WorkflowHistory> subHistories = new ArrayList<>();
    private ActivityExecutionStatus currentState;
    private ActivityMultiInstanceState multiInstanceState;
    @JsonIgnore
    private ClientError exception;
    private int completeCount;
    private Variables environment;
    private Variables activityInput;
    private Variables filteredInput;
    private Variables filteredOutput;
    private long startTimeMillis;
    private long completeTimeMillis;
    
  }

}
