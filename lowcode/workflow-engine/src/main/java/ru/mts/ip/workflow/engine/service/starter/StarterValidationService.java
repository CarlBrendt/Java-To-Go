package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.XsdValidation;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

import java.util.List;

public interface StarterValidationService {
  List<ErrorDescription> validateStarterCompatibleWithDefinition(JsonNode json, JsonNode workflowInputValidateSchema,
      List<XsdValidation.VariableToValidate> variablesToValidate);
}
