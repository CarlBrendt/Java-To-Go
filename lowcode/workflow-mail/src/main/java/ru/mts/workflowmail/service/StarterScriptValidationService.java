package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowmail.service.dto.MailConsumerForInternal;
import ru.mts.workflowmail.share.script.ClientError;
import ru.mts.workflowmail.share.script.ClientErrorDescription;
import ru.mts.workflowmail.share.script.ScriptExecutionContext;

import java.util.List;
import java.util.Optional;

public interface StarterScriptValidationService {
  Optional<ClientError> validateOutputTemplateValueReplacement(JsonNode value, ScriptExecutionContext ctx);
  List<ClientErrorDescription> validateJson(JsonNode json, JsonNode schema);

  Optional<JsonNode> transformMailOutput(JsonNode mailMessageJson, JsonNode outputTemplate);

  ScriptExecutionContext compileScriptContext(MailConsumerForInternal consumer);
}
