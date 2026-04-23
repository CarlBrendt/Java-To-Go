package ru.mts.ip.workflow.engine.temporal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.ActivityMultiInstanceState;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ActivityExecutionStatus;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ExecutionStat;

@Data
public class InstanceHistory {

  private Collection<ActivityExecutionState> activityStates = new ArrayList<>();
  private WorkflowDefinition workflowDefinition;
  private String status;
  private JsonNode initVariables;
  private JsonNode continuedVariables;
  private String startTime;
  private String endTime;
  
  public Collection<ActivityExecutionState> getInProgress() {
    return activityStates.stream().filter(s -> s.getStatus() == ActivityExecutionStatus.IN_PROGRESS).toList();
  }

  
  @Data
  @NoArgsConstructor
  public static class ActivityExecutionState {
    private String activityId;
    private String aggregateActivityId;
    private int completeCount;
    private ActivityMultiInstanceState multiInstanceState;
    private ActivityExecutionStatus status;
    private ActivityErrorDetails progressDetails;
    private ActivityErrorDetails errorDetails;
    @JsonProperty("startTime")
    private String startTime;
    @JsonProperty("completeTime")
    private String completeTime;
    private JsonNode environment;
    private JsonNode activityInput;
    private JsonNode filteredInput;
    private JsonNode filteredOutput;
    
    public ActivityExecutionState(ExecutionStat stat) {
      aggregateActivityId = stat.getAggregateActivityId();
      activityId = stat.getActivityid();
      completeCount = stat.getCompleteCount();
      status = stat.getCurrentState();
      multiInstanceState = stat.getMultiInstanceState();
      environment = Optional.ofNullable(stat.getEnvironment()).map(Variables::asNode).orElse(null);
      activityInput = Optional.ofNullable(stat.getActivityInput()).map(Variables::asNode).orElse(null);
      filteredInput = Optional.ofNullable(stat.getFilteredInput()).map(Variables::asNode).orElse(null);
      filteredOutput = Optional.ofNullable(stat.getFilteredOutput()).map(Variables::asNode).orElse(null);
      
      long startTimeMillis = stat.getStartTimeMillis();
      long completeTimeMillis = stat.getCompleteTimeMillis();
      
      if(startTimeMillis > 0) {
        startTime = DateHelper.asTextISO(Instant.ofEpochMilli(stat.getStartTimeMillis()));
      }
      
      if(completeTimeMillis  > 0) {
        completeTime = DateHelper.asTextISO(Instant.ofEpochMilli(stat.getCompleteTimeMillis()));
      }
      
    }

    public void setStartTime(String startTime){
      this.startTime = startTime;
    }

    @JsonIgnore
    public void setStartTime(long timeMillis){
      if (timeMillis > 0) {
        this.setStartTime(DateHelper.asTextISO(Instant.ofEpochMilli(timeMillis)));
      }
    }


    @JsonIgnore
    public void setCompleteTime(long timeMillis){
      if (timeMillis > 0) {
        this.setCompleteTime(DateHelper.asTextISO(Instant.ofEpochMilli(timeMillis)));
      }
    }



    public void setCompleteTime(String completeTime){
      this.completeTime = completeTime;
    }
  }

  @Data
  @Accessors(chain = true)
  public static class ActivityProgressDetails {
    private String exceptionMessage;
    private String stackTrace;
    private int tryCount;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ActivityErrorDetails {
    private String exceptionMessage;
    private String stackTrace;
    private Integer tryCount;
    private ActivityErrorDetails cause;
  }
  
  public void addActivityExecutionStat(ActivityExecutionState stat) {
    activityStates.add(stat);
  }

  
}
