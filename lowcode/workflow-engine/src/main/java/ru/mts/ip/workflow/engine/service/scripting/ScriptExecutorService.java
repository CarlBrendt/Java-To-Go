package ru.mts.ip.workflow.engine.service.scripting;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext;

public interface ScriptExecutorService {
  ResolvePlaceholdersExecutionResult resolvePlaceholders(ResolvePlaceholdersExecutionContext ctx);
  JsonNode executeScript(String script, ScriptExecutionContext ctx);
  boolean isExecutable(String script);
  Map<String, JsonNode> resolvePlaceholders(Map<String, JsonNode> node, ScriptExecutionContext ctx);
  JsonNode resolvePlaceholders(JsonNode node, ScriptExecutionContext ctx);
  ResponseEntity<String> resolvePlaceholdersProxy(ReqResolvePlaceholdersExecutionContext request);
  <T> T resolvePlaceholders(T object, Class<T> clazz, ScriptExecutionContext ctx);
  JsonNode filter(Map<String, String> filter, ScriptExecutionContext ctx, JsonNode result);
  ScriptExecutionContext injectActivityArgsToContext(Map<String, JsonNode> args, ScriptExecutionContext ctx);
}
