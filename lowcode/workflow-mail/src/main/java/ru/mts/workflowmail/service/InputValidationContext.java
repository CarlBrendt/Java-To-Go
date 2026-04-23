package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class InputValidationContext {

  private JsonNode inputValidateSchema;
  private JsonNode validationTarget;
  private List<PropertyViolation> propertyViolations = new ArrayList<>();

  public void addError(PropertyViolation violation) {
    propertyViolations.add(violation);
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

}
