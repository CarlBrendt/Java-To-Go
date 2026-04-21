package ru.mts.ip.workflow.engine.temporal;

import java.util.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.temporal.api.filter.v1.StartTimeFilter;
import io.temporal.api.filter.v1.WorkflowExecutionFilter;
import io.temporal.api.filter.v1.WorkflowTypeFilter;
import io.temporal.api.workflowservice.v1.CountWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import lombok.Data;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.service.SerializeUtils;

@Data
public class WorkflowInstanceSearch {
  
  private String pageToken;
  private Integer pageSize;
  private String startingTimeFrom;
  private String startingTimeTo;
  private String workflowName;
  private String businessKey;
  private String executionStatus;

  public ListWorkflowExecutionsRequest asListWorkflowExecutionsRequest() {
    var builder = ListWorkflowExecutionsRequest.newBuilder();
    builder.setNamespace(Const.DEFAULT_TENANT_ID);
    Optional.ofNullable(getPageToken())
      .filter(v -> !v.isBlank())
      .ifPresent(pagaToken -> builder.setNextPageToken(ByteString.copyFrom(SerializeUtils.decodeNextPageToken(pagaToken))));
    Optional.ofNullable(getPageSize())
      .ifPresent(pageSize -> builder.setPageSize(pageSize));
    builder.setQuery(VisibilityQueryUtils.toQuery(this));
    return builder.build();
  }
  
  public CountWorkflowExecutionsRequest asCountWorkflowExecutionsRequest() {
    var builder = CountWorkflowExecutionsRequest.newBuilder();
    builder.setNamespace(Const.DEFAULT_TENANT_ID);
    builder.setQuery(VisibilityQueryUtils.toQuery(this));
    return builder.build();
  }
  
  public ListClosedWorkflowExecutionsRequest asListClosedWorkflowExecutionsRequest() {
    var businessKey = getBusinessKey();
    var workflowName = getWorkflowName();
    var builder = ListClosedWorkflowExecutionsRequest.newBuilder();
    builder.setNamespace(Const.DEFAULT_TENANT_ID);
    Optional.ofNullable(getPageToken())
      .filter(v -> !v.isBlank())
      .ifPresent(pagaToken -> builder.setNextPageToken(ByteString.copyFrom(SerializeUtils.decodeNextPageToken(pagaToken))));
    Optional.ofNullable(getPageSize())
      .ifPresent(pageSize -> builder.setMaximumPageSize(pageSize));
    
    if (businessKey != null && !businessKey.isBlank()) {
      builder.setExecutionFilter(WorkflowExecutionFilter.newBuilder().setWorkflowId(businessKey));
    }    
    
    if (workflowName != null && !workflowName.isBlank()) {
      builder.setTypeFilter(WorkflowTypeFilter.newBuilder().setName(workflowName).build());
    }    
    
    builder.setStartTimeFilter(
      StartTimeFilter.newBuilder()
        .setEarliestTime(startingTimeFromAsTimestamp())
        .setLatestTime(startingTimeToAsTimestamp())
      .build()
    );
    
    return builder.build();
  }
  
  private Timestamp startingTimeFromAsTimestamp() {
    var startingFrom = Optional.ofNullable(getStartingTimeFrom()).orElse("2000-01-01T00:00:00.000Z");
    var startingFromInstant = DateHelper.parseISO(startingFrom).toInstant();
    return  Timestamp.newBuilder().setSeconds(startingFromInstant.getEpochSecond()).setNanos(startingFromInstant.getNano()).build();
  }

  private Timestamp startingTimeToAsTimestamp() {
    var startingTo = Optional.ofNullable(getStartingTimeTo()).orElse("3000-01-01T00:00:00.000Z");
    var startingToInstant = DateHelper.parseISO(startingTo).toInstant();
    return Timestamp.newBuilder().setSeconds(startingToInstant.getEpochSecond()).setNanos(startingToInstant.getNano()).build();
  }
  
  public ListOpenWorkflowExecutionsRequest asListOpenWorkflowExecutionsRequest() {
    var businessKey = getBusinessKey();
    var workflowName = getWorkflowName();
    var builder = ListOpenWorkflowExecutionsRequest.newBuilder();
    builder.setNamespace(Const.DEFAULT_TENANT_ID);
    Optional.ofNullable(getPageToken())
      .filter(v -> !v.isBlank())
      .ifPresent(pagaToken -> builder.setNextPageToken(ByteString.copyFrom(SerializeUtils.decodeNextPageToken(pagaToken))));
    Optional.ofNullable(getPageSize())
      .ifPresent(pageSize -> builder.setMaximumPageSize(pageSize));

    if (businessKey != null && !businessKey.isBlank()) {
      builder.setExecutionFilter(WorkflowExecutionFilter.newBuilder().setWorkflowId(businessKey));
    }

    if (workflowName != null && !workflowName.isBlank()) {
      builder.setTypeFilter(WorkflowTypeFilter.newBuilder().setName(workflowName).build());
    }    
    
    builder.setStartTimeFilter(
      StartTimeFilter.newBuilder()
        .setEarliestTime(startingTimeFromAsTimestamp())
        .setLatestTime(startingTimeToAsTimestamp())
      .build()
    );
    
    return builder.build();
  }
  
  
}
