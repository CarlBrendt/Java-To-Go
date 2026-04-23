package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowscheduler.share.script.ClientError;
import ru.mts.workflowscheduler.share.script.ClientErrorDescription;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext;

import java.util.List;
import java.util.Optional;

public interface StarterScriptValidationService {
  Optional<ClientError> validateOutputTemplateValueReplacement(JsonNode value, ScriptExecutionContext ctx);
  List<ClientErrorDescription> validateJson(JsonNode json, JsonNode schema);
}
