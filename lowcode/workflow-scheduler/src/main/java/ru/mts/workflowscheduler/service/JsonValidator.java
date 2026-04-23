package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowscheduler.share.script.ClientErrorDescription;

import java.util.List;

public interface JsonValidator {
  List<ClientErrorDescription> validateVariables(JsonNode json, JsonNode schema);
}
