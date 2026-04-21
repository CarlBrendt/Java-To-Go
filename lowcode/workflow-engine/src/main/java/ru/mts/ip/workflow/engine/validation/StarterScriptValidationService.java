package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;

import java.util.Optional;

public interface StarterScriptValidationService {
  Optional<ClientError> validateOutputTemplateValueReplacement(JsonNode value, ScriptExecutionContext ctx);
}
