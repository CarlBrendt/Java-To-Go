package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowmail.share.script.ResolvePlaceholdersExecutionContext;
import ru.mts.workflowmail.share.script.ResolvePlaceholdersExecutionResult;
import ru.mts.workflowmail.share.script.ScriptExecutionContext;

import java.util.Map;

public interface ScriptExecutorService {
  ResolvePlaceholdersExecutionResult resolvePlaceholders(ResolvePlaceholdersExecutionContext ctx);
  JsonNode executeScript(String script, ScriptExecutionContext ctx);
  boolean isExecutable(String script);
  Map<String, JsonNode> resolvePlaceholders(Map<String, JsonNode> node, ScriptExecutionContext ctx);
  JsonNode resolvePlaceholders(JsonNode node, ScriptExecutionContext ctx);
  <T> T resolvePlaceholders(T object, Class<T> clazz, ScriptExecutionContext ctx);
  JsonNode filter(Map<String, String> filter, ScriptExecutionContext ctx, JsonNode result);
  ScriptExecutionContext injectActivityArgsToContext(Map<String, JsonNode> args, ScriptExecutionContext ctx);
}
