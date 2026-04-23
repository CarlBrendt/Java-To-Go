package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResErrorDescription;

@Data
@JsonInclude(Include.NON_NULL)
public class ResDebugExecutionReport {
  
  private ResActivityExecution execution;
  private JsonNode output;

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResActivityExecution {
    private String activityId;
    private JsonNode environment;
    private JsonNode filteredInput;
    private ResWorkflowExecution workflowExecution;
    private JsonNode filteredOutput;
    private List<ResErrorDescription> errors; 
    private List<ResActivityExecution> parallelExecutions;
    private ResActivityExecution nextExecution;
  }
  
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResWorkflowExecution {
    private ResPreparedRestCall preparedRestCall;
    private JsonNode preparedTransfor;
    private JsonNode preparedSubWorkflowExecution;
    private JsonNode preparedAwaitWorMessage;
    private JsonNode preparedDbCall;
    private JsonNode preparedXsltTransform;
    private JsonNode preparedSapCall;
    private JsonNode preparedRabbitmqCall;
    private Boolean retryableStuck;
    private JsonNode output;
  }
  
  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResPreparedRestCall {
    private String method;
    private String url;
    private JsonNode body;
    private Map<String, List<String>> headers;
  }
  

}
