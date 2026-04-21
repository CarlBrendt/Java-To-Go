package ru.mts.ip.workflow.engine.temporal;

import static ru.mts.ip.workflow.engine.Const.WorkflowType.DB_CALL;
import static ru.mts.ip.workflow.engine.Const.WorkflowType.REST_CALL;
import static ru.mts.ip.workflow.engine.Const.WorkflowType.XSLT_TRANSFORM;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.failure.v1.Failure;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflow.v1.PendingActivityInfo;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.common.WorkflowExecutionHistory;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ActivityExecutionStatus;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory.ActivityErrorDetails;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory.ActivityExecutionState;

@Slf4j
public class TemporalHistoryHelper {

  private final Map<String, ActivityTaskEvent> activityInfoMapping = new HashMap<>();
  private final DescribeWorkflowExecutionResponse describe;
  private static final ObjectMapper OM = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new Jdk8Module());;
  private final WorkflowDefinition workflowDefinition;

  public TemporalHistoryHelper(WorkflowExecutionHistory history,
      DescribeWorkflowExecutionResponse describe) {
    this.describe = describe;
    this.workflowDefinition = extractWorkflowDefinition(history);
    initActivityInfoMapping(history, describe);
  }

  private WorkflowDefinition extractWorkflowDefinition(WorkflowExecutionHistory history) {
    return history.getEvents()
        .stream()
        .filter(e -> e.getEventType() == EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED)
        .findFirst()
        .map(event -> {return event.getWorkflowExecutionStartedEventAttributes().getInput().getPayloads(0).getData();})
        .flatMap(this::parseDefinition)
        .orElse(null);
  }

  @Data
  @Accessors(chain = true)
  private static class ActivityTaskEvent {
    private HistoryEvent scheduledEvent;
    private HistoryEvent failedEvent;
    private HistoryEvent completedEvent;
    private HistoryEvent startedEvent;
    private PendingActivityInfo pendingInfo;

    private ActivityErrorDetails getActivityProgressDetails(
        PendingActivityInfo pendingInfoLocal) {
      var failure = pendingInfoLocal.getLastFailure();
      String message = null;
      String stackTrace = null;
      message = failure.getMessage();
      stackTrace = failure.getStackTrace();
      failure = failure.getCause();
      if (failure != null && "JavaSDK".equals(failure.getSource())) {
        message = failure.getMessage();
        stackTrace = failure.getStackTrace();
      }
      var errDetails = new ActivityErrorDetails().setExceptionMessage(message.isBlank() ? null : message)
          .setStackTrace(stackTrace.isBlank() ? null : stackTrace)
          .setTryCount(pendingInfoLocal.getAttempt() - 1);

      if (failure.hasCause()){
        var cause = new ActivityErrorDetails();
        fillErrorDetails(cause, failure.getCause());
        errDetails.setCause(cause);
      }
      return errDetails;
    }

    void appendInProgressDetails(ActivityExecutionState state, InstanceHistory hist) {
      if (pendingInfo != null) {
        if (pendingInfo.getAttempt() > 1) {
          var progressDetails= getActivityProgressDetails(pendingInfo);
          state.setProgressDetails(progressDetails);
          if (Const.WorkflowInstanceStatus.COMPLETED.equals(hist.getStatus())) {
            state.setStatus(ActivityExecutionStatus.STOPPED);
          } else if (Const.WorkflowInstanceStatus.TIMED_OUT.equals(hist.getStatus())) {
            state.setStatus(ActivityExecutionStatus.ERROR);
          }
        } else if (Const.WorkflowInstanceStatus.COMPLETED.equals(hist.getStatus())){
          state.setStatus(ActivityExecutionStatus.IGNORED);
        }
      }
      if (Const.WorkflowInstanceStatus.CANCELED.equals(hist.getStatus())) {
        state.setStatus(ActivityExecutionStatus.CANCELED);
      }
    }

    public void appendErrorDetails(ActivityExecutionState state, InstanceHistory history) {
      if (failedEvent != null) {
        var failure = failedEvent.getActivityTaskFailedEventAttributes().getFailure();

        var error = new ActivityErrorDetails();
        fillErrorDetails(error, failure);
        error.setTryCount(startedEvent.getActivityTaskStartedEventAttributes().getAttempt());

        state.setErrorDetails(error);

        if (!Const.WorkflowInstanceStatus.CANCELED.equals(history.getStatus()) &&
            !Const.WorkflowInstanceStatus.COMPLETED.equals(history.getStatus())) {
          state.setStatus(ActivityExecutionStatus.ERROR);
        }

      } else if (!Const.WorkflowInstanceStatus.RUNNING.equals(history.getStatus()) && state.getProgressDetails() != null) {
        state.setErrorDetails(state.getProgressDetails());
        state.setProgressDetails(null);
      }
    }

    private void fillErrorDetails(ActivityErrorDetails errorDetails, Failure failure) {
      if (errorDetails == null || failure == null) {
        return;
      }
      errorDetails.setExceptionMessage(failure.getMessage())
          .setStackTrace(failure.getStackTrace());

      // Рекурсивно обрабатываем причину (cause)
      if (failure.hasCause()) {
        var cause = new ActivityErrorDetails();
        errorDetails.setCause(cause);
        fillErrorDetails(cause, failure.getCause());
      }
    }

    @SuppressWarnings("unused")
    private void propagateErrorStatus(String aggregateActivityId, InstanceHistory hist) {
      if (aggregateActivityId != null) {
        for (var state : hist.getActivityStates()) {
          if (aggregateActivityId.equals(state.getActivityId())) {
            state.setStatus(ActivityExecutionStatus.ERROR);
            propagateErrorStatus(state.getAggregateActivityId(), hist);
            break;
          }
        }
      }
    }
  }

  private Optional<ActivityTaskEvent> findByScheduledEventId(long id) {
    return activityInfoMapping.values()
        .stream()
        .filter(e -> e.getScheduledEvent().getEventId() == id)
        .findFirst();
  }

  private void initActivityInfoMapping(WorkflowExecutionHistory history,
      DescribeWorkflowExecutionResponse desc) {
    var temporalPendingActivitiesList = desc.getPendingActivitiesList();

    history.getEvents().forEach(event -> {
      if (event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED) {
        var attributes = event.getActivityTaskScheduledEventAttributes();
        var inputPayload = attributes.getInput();
        var temporalActivityId = attributes.getActivityId();
        parseActivityId(inputPayload.getPayloads(0).getData()).ifPresent(id -> {
          var scheduledEvent = new ActivityTaskEvent().setScheduledEvent(event);
          activityInfoMapping.put(id, scheduledEvent);
          for (var activityInfo : temporalPendingActivitiesList) {
            if (activityInfo.getActivityId().equals(temporalActivityId)) {
              scheduledEvent.setPendingInfo(activityInfo);
              break;
            }
          }
        });
      }
    });

    history.getEvents().forEach(event -> {
      if (event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED) {
        var atts = event.getActivityTaskFailedEventAttributes();
        var scheduledEventId = atts.getScheduledEventId();
        findByScheduledEventId(scheduledEventId).ifPresent(scheduledEvent -> {
          scheduledEvent.setFailedEvent(event);
        });
      } else if (event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED) {
        var atts = event.getActivityTaskCompletedEventAttributes();
        var scheduledEventId = atts.getScheduledEventId();
        findByScheduledEventId(scheduledEventId).ifPresent(scheduledEvent -> {
          scheduledEvent.setCompletedEvent(event);
        });
      }else if (event.getEventType() == EventType.EVENT_TYPE_ACTIVITY_TASK_STARTED) {
        var atts = event.getActivityTaskStartedEventAttributes();
        var scheduledEventId = atts.getScheduledEventId();
        findByScheduledEventId(scheduledEventId).ifPresent(scheduledEvent -> {
          scheduledEvent.setStartedEvent(event);
        });
      }
    });
  }

  private void appendInProgressDetails(ActivityExecutionState state, InstanceHistory history) {
    Optional.ofNullable(activityInfoMapping.get(state.getActivityId()))
        .ifPresent(event -> event.appendInProgressDetails(state, history));
  }

  private void appendErrorDetails(ActivityExecutionState state, InstanceHistory history) {
    Optional.ofNullable(activityInfoMapping.get(state.getActivityId()))
        .ifPresent(event -> event.appendErrorDetails(state, history));
  }

  public void appendDetails(InstanceHistory history) {
    Collection<ActivityExecutionState> inProgress = history.getInProgress();
    Collection<ActivityExecutionState> allStates = history.getActivityStates();
    getWorkflowExecutionStatus().ifPresent(history::setStatus);
    inProgress.forEach(inp -> appendInProgressDetails(inp, history));
    allStates.forEach(state -> appendErrorDetails(state, history));
    getStartTime().ifPresent(history::setStartTime);
    getEndTime().ifPresent(history::setEndTime);

    history.getActivityStates()
        .stream().filter(f -> f.getActivityInput() == null || f.getActivityInput().isNull())
        .forEach(state -> state.setActivityInput(getActivityInput(state.getActivityId())));

    if (history.getWorkflowDefinition() == null) {history.setWorkflowDefinition(this.workflowDefinition);}
  }

  public InstanceHistory instanceHistory() {
    InstanceHistory history = new InstanceHistory();
    activityInfoMapping.entrySet()
        .stream()
        .map(e -> toActivityExState(e.getKey(), e.getValue()))
        .forEach(history::addActivityExecutionStat);
    history.setWorkflowDefinition(this.workflowDefinition);
    return history;
  }

  private ActivityExecutionState toActivityExState(String activityId, ActivityTaskEvent event) {
    var stat = new ActivityExecutionState();
    stat.setActivityId(activityId);

    var scheduledEvent = event.getScheduledEvent();
    var completedEvent = event.getCompletedEvent();

      stat.setStartTime(Timestamps.toMillis(scheduledEvent.getEventTime()));

    var schedAttrs = scheduledEvent.getActivityTaskScheduledEventAttributes();
    var schedTypeName = schedAttrs.getActivityType().getName();
    if (completedEvent != null) {
      stat.setStatus(ActivityExecutionStatus.COMPLETED);
      stat.setCompleteTime(Timestamps.toMillis(completedEvent.getEventTime()));
      var completedAttr = completedEvent.getActivityTaskCompletedEventAttributes();
      if (completedAttr.getResult().getPayloadsCount() > 0) {
        var outputData = completedAttr.getResult().getPayloads(0).getData();
        stat.setFilteredOutput(parseActivityOutput(outputData, schedTypeName).asNode());
      }
    }
    return stat;
  }

  private JsonNode getActivityInput(String activityId) {
    var event = activityInfoMapping.get(activityId);
    if (event == null) {
      return OM.nullNode();
    }
    var scheduledEvent = event.getScheduledEvent();
    var schedAttrs = scheduledEvent.getActivityTaskScheduledEventAttributes();
    var schedTypeName = schedAttrs.getActivityType().getName();
    var inputData = schedAttrs.getInput().getPayloads(0).getData();
    return parseActivityInput(inputData, schedTypeName);
  }

  private Variables parseActivityOutput(ByteString byteString, String workflowType) {
    JsonNode output;
    Variables variables;
    try {
      if (workflowType.equals(REST_CALL)) {
        output = OM.valueToTree(OM.readValue(byteString.toByteArray(), RestCallOutput.class));
        variables = new Variables(output);
      } else if (workflowType.equals(DB_CALL)) {
        var dbResult = OM.readValue(byteString.toByteArray(), JsonNode.class);
        variables = new Variables();
        variables.put("databaseCallResult", dbResult);
      } else if (workflowType.equals(XSLT_TRANSFORM)) {
        variables = new Variables();
        var xsltResult = OM.readValue(byteString.toByteArray(), JsonNode.class);
        variables.put("xsltTransformResult", xsltResult);
      } else {
        output = OM.readValue(byteString.toByteArray(), JsonNode.class);
        variables = new Variables(output);
      }
    } catch (IOException ex) {
      log.error("ActivityOutput parsing", ex);
      variables = new Variables();
    }
    return variables;
  }

  private JsonNode parseActivityInput(ByteString byteString, String workflowType) {
    if (workflowType == null) {
      return OM.nullNode();
    }
    try {
      switch (workflowType) {
        case REST_CALL -> {
          return OM.valueToTree(OM.readValue(byteString.toByteArray(), RestCallInput.class));
        }
        case Const.WorkflowType.SEND_TO_KAFKA -> {
          return OM.valueToTree(OM.readValue(byteString.toByteArray(), SendToKafkaInput.class));
        }
        case Const.WorkflowType.SEND_TO_RABBITMQ -> {
          return OM.valueToTree(OM.readValue(byteString.toByteArray(), SendToRabbitmqInput.class));
        }
        case Const.WorkflowType.SEND_TO_S3 -> {
          return OM.valueToTree(OM.readValue(byteString.toByteArray(), SendToS3Input.class));
        }
        case Const.WorkflowType.SEND_TO_SAP -> {
          return OM.valueToTree(OM.readValue(byteString.toByteArray(), SendToSapInput.class));
        }
      }
    } catch (IOException ex) {
      log.error("ActivityInput parsing", ex);
    }
    return OM.nullNode();
  }


  @JsonIgnoreProperties(ignoreUnknown = true)
  record ActivityIdField(String activityId) {
  }

  private Optional<String> parseActivityId(ByteString byteString) {
    try {
      return Optional.ofNullable(
          OM.readValue(byteString.toByteArray(), ActivityIdField.class).activityId);
    } catch (IOException ex) {
      log.error("ActivityIdField parsing", ex);
      return Optional.empty();
    }
  }

  private Optional<WorkflowDefinition> parseDefinition(ByteString byteString) {
    try {
      return Optional.ofNullable(
          OM.readValue(byteString.toByteArray(), JsonNode.class))
           .filter(f -> f.has("workflowDefinition"))
           .map(node -> OM.convertValue(node.get("workflowDefinition"), WorkflowDefinition.class));
    } catch (IOException ex) {
      log.error("WorkflowDefinition parsing", ex);
      return Optional.empty();
    }
  }

  public Optional<String> getWorkflowExecutionStatus() {
    return Const.WorkflowInstanceStatus.ofTemporalInternal(
        describe.getWorkflowExecutionInfo().getStatus().toString());
  }

  public Optional<String> getStartTime() {
    return Optional.ofNullable(describe.getWorkflowExecutionInfo())
        .map(WorkflowExecutionInfo::getStartTime)
        .filter(ts -> ts.getSeconds() != 0)
        .map(DateHelper::asTextISO);
  }

  public Optional<String> getEndTime() {
    return Optional.ofNullable(describe.getWorkflowExecutionInfo())
        .map(WorkflowExecutionInfo::getCloseTime)
        .filter(ts -> ts.getSeconds() != 0)
        .map(DateHelper::asTextISO);
  }

}
