package ru.mts.ip.workflow.engine.temporal;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VisibilityQueryUtils {

  public String toQuery(WorkflowInstanceSearch searchConfig) {
    StringBuilder sb = new StringBuilder();
    var businessKey = searchConfig.getBusinessKey();
    var startingFrom = Optional.ofNullable(searchConfig.getStartingTimeFrom()).orElse("2000-01-01T00:00:00.000Z");
    var startingTo = Optional.ofNullable(searchConfig.getStartingTimeTo()).orElse("3000-01-01T00:00:00.000Z");
    var workflowName = searchConfig.getWorkflowName();
    sb.append("StartTime BETWEEN '%s' AND '%s'".formatted(startingFrom, startingTo));
    if(workflowName != null && !workflowName.isBlank()) {
      sb.append(" AND WorkflowType = '%s'".formatted(workflowName));
    } else if(businessKey != null && !businessKey.isBlank()) {
      sb.append(" AND WorkflowId = '%s'".formatted(businessKey));
    }
    return sb.toString();
  }
  
  
  
  
}
