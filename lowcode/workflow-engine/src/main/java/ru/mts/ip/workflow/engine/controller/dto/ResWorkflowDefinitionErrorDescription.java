package ru.mts.ip.workflow.engine.controller.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(Include.NON_NULL)
public class ResWorkflowDefinitionErrorDescription {

  private ResWorkflowDefinition workflowDefinition;
  private List<ResErrorDescription> errors = new ArrayList<>();

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResErrorLocation {
    private String fieldPath;
    private String activityId;
    private List<String> executionPath;
    private String nextTransition;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResStarterErrorContext {
    private UUID id;
    private String type;
    private String name;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResErrorContext {
    private Object rejectedValue;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResScriptExecutionContext {
    private Map<String, JsonNode> variables;
    private ResScriptWorkflowView wf;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResScriptErrorContext {
    private JsonNode variableContext;
    private ResScriptWorkflowView wf;
    private String rejectedScript;
    private String systemMessage;
    private List<String> unknownVariables;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResInputValidationContext {
    private JsonNode inputValidateSchema;
    private JsonNode validationTarget;
    private List<ResPropertyViolation> propertyViolations;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResInfinityCyclesContext {
    private List<List<String>> infinityCycles;
  }
  
  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResPropertyViolation{
    private String propertyRootPath;
    private String constraintPath;
    private String propertyName;
    private String systemMessage;
    private String constraintType;
    private Map<String, Object> details;
  }

  @Data
  @Accessors(chain = true)
  @JsonInclude(Include.NON_NULL)
  public static class ResScriptWorkflowView{
    private String businessKey;
    private Instant workflowExpiration;
    private Map<String, String> secrets;
    private Map<String, JsonNode> initVariables;
    private Map<String, List<JsonNode>> consumedMessages;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResErrorDescription {
    private String code;
    private String level;
    private String message;
    private String systemMessage;
    private String solvingAdviceMessage;
    private String solvingAdviceUrl;
    private ResStarterErrorContext starter;
    private ResErrorLocation location;
    private ResErrorContext  context;
    private ResScriptErrorContext scriptContext;
    private ResInputValidationContext inputValidationContext;
    private ResInfinityCyclesContext infinityCyclesContext;
    private List<ResErrorDescription> cause;
    
  }

}
