package ru.mts.ip.workflow.engine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class YamlValidation {

  private List<VariableToValidate> variablesToValidate = new ArrayList<>();

  public boolean isEmpty() {
    return variablesToValidate.isEmpty();
  }

  public YamlValidation copy() {
    YamlValidation copy = new YamlValidation();

    if (this.variablesToValidate != null) {
      copy.variablesToValidate = this.variablesToValidate.stream()
          .map(VariableToValidate::copy)
          .toList();
    }

    return copy;
  }

  @Data
  public static class VariableToValidate {
    private String variableName;
    private JsonNode jsonSchema;

    public VariableToValidate copy() {
      VariableToValidate copy = new VariableToValidate();
      copy.variableName = this.variableName;
      if (this.jsonSchema != null) {
        copy.jsonSchema = this.jsonSchema.deepCopy();
      }
      return copy;
    }
  }

}
