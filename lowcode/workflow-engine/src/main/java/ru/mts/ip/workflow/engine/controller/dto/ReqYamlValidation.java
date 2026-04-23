package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReqYamlValidation {

  private List<ReqVariableToValidate> variablesToValidate = new ArrayList<>();

  public ReqYamlValidation copy() {
    ReqYamlValidation copy = new ReqYamlValidation();

    if (this.variablesToValidate != null) {
      copy.variablesToValidate = this.variablesToValidate.stream()
          .map(ReqVariableToValidate::copy)
          .toList();
    }

    return copy;
  }

  @Data
  public static class ReqVariableToValidate {
    private String variableName;
    private JsonNode jsonSchema;

    public ReqVariableToValidate copy() {
      ReqVariableToValidate copy = new ReqVariableToValidate();
      copy.variableName = this.variableName;
      if (this.jsonSchema != null) {
        copy.jsonSchema = this.jsonSchema.deepCopy();
      }
      return copy;
    }
  }

}
