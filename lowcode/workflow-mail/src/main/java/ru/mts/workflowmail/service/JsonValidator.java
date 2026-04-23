package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowmail.share.script.ClientErrorDescription;

import java.util.List;

public interface JsonValidator {
  List<ClientErrorDescription> validateVariables(JsonNode json, JsonNode schema);
}
