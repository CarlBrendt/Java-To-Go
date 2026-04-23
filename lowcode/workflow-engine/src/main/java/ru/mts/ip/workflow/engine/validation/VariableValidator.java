package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.YamlValidation;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.service.Variables;

import java.util.List;

public interface VariableValidator {
  List<ClientErrorDescription> validateVariables(JsonNode schema, Variables variables);

  List<ClientErrorDescription> validateStringVariables(List<String> requiredVariables, Variables variables);

  List<ClientErrorDescription> validateYamlVariables(JsonNode variables,YamlValidation yamlValidation);
}
