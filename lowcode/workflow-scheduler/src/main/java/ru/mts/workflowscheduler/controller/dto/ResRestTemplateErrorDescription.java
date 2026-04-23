package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext.ScriptWorkflowView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(Include.NON_NULL)
public class ResRestTemplateErrorDescription {

  private List<ResErrorDescription> errors = new ArrayList<>();

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResErrorLocation {
    private String fieldPath;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResErrorContext {
    private Object rejectedValue;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResScriptErrorContext {
    private JsonNode variableContext;
    private ScriptWorkflowView wf;
    private String rejectedScript;
    private String systemMessage;
    private List<String> unknownVariables;
  }

  @Data
  @JsonInclude(Include.NON_NULL)
  public static class ResInputValidationContext {
    private JsonNode inputValidateSchema;
    private JsonNode validationTarget;
    private List<PropertyViolation> propertyViolations = new ArrayList<>();
    public void addError(PropertyViolation violation) {
      propertyViolations.add(violation);
    }
  }

  @Data
  @Accessors(chain = true)
  public static class PropertyViolation {
    private String propertyRootPath;
    private String constraintPath;
    private String propertyName;
    private String systemMessage;
    private String constraintType;
    private Map<String, Object> details;
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
    private ResErrorLocation location;
    private ResErrorContext  context;
    private ResScriptErrorContext scriptContext;
    private ResInputValidationContext inputValidationContext;
    private List<ResErrorDescription> cause;
  }

}
