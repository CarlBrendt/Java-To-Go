package ru.mts.ip.workflow.engine.controller.dto;

import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ActivityExecutionStatus;
import ru.mts.ip.workflow.engine.temporal.WorkflowConsumedMessages;

@Data
public class ResInstanceHistory {

  private Collection<ResActivityExecutionState> activityStates;
  private ResWorkflowDefinition workflowDefinition;
  private WorkflowConsumedMessages consumedMessages;
  private String status;
  private JsonNode initVariables;
  private String startTime;
  private String endTime;
  
  @Data
  public static class ResActivityExecutionState {
    private String activityId;
    private int completeCount;
    private ActivityExecutionStatus status;
    private ResActivityMultiInstanceState multiInstanceState;
    private ResActivityErrorDetails progressDetails;
    private ResActivityErrorDetails errorDetails;
    private String startTime;
    private String completeTime;

    private JsonNode environment;
    private JsonNode activityInput;
    private JsonNode filteredInput;
    private JsonNode filteredOutput;
  }
  
  @Data
  @Accessors(chain = true)
  public static class ResActivityMultiInstanceState {
    private List<JsonNode> resultCollection;
    private String resultCollectionRef;
    private Integer completeCount;
    private Integer offset;
    private Integer size;
  }
  
  @Data
  public static class ResActivityProgressDetails {
    private String exceptionMessage;
    private String stackTrace;
    private int tryCount;
  }
  
  @Data
  @Accessors(chain = true)
  public static class ResActivityErrorDetails {
    private String exceptionMessage;
    private String stackTrace;
    private Integer tryCount;
    private ResActivityErrorDetails cause;
  }
  
}
