package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.Starter;

public interface ScriptContextCompiler {
  JsonNode compileDefaultVariables(Starter starter);

  JsonNode getOutputTemplate(Starter starter);
}
