package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.mapper.DtoMapper;
import ru.mts.workflowscheduler.share.script.ResolvePlaceholdersExecutionContext;
import ru.mts.workflowscheduler.share.script.ResolvePlaceholdersExecutionResult;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScriptExecutorServiceImpl implements ScriptExecutorService{

  private final ScriptExecutorClient scriptExecutorClient;
  private final DtoMapper mapper;
  private final static ObjectMapper OM = new ObjectMapper();


  @Override
  public ResolvePlaceholdersExecutionResult resolvePlaceholders(
      ResolvePlaceholdersExecutionContext ctx) {
    var resp = scriptExecutorClient.resolvePlaceholders(mapper.toResolvePlaceholdersExecutionContext(ctx));
    return mapper.toResolvePlaceholdersExecutionResult(resp);
  }

  @Override
  public JsonNode executeScript(String script, ScriptExecutionContext ctx) {
    ResolvePlaceholdersExecutionContext evalArgs = new ResolvePlaceholdersExecutionContext();
    evalArgs.setNode(OM.valueToTree(script));
    evalArgs.setScriptContext(ctx);
    ResolvePlaceholdersExecutionResult res = resolvePlaceholders(evalArgs);
    return res.getResultNode();
  }

  private ResolvePlaceholdersExecutionContext compileExecutionContext(Map<String, JsonNode> node, ScriptExecutionContext ctx) {
    ResolvePlaceholdersExecutionContext evalArgs = new ResolvePlaceholdersExecutionContext();
    evalArgs.setNode(OM.valueToTree(node));
    evalArgs.setScriptContext(ctx);
    return evalArgs;
  }

  @Override
  public boolean isExecutable(@NonNull String script) {
    ScriptExecutorClient.ResIsExecutable res = scriptExecutorClient.isExecutable(new ScriptExecutorClient.ReqIsExecutable().setScript(script));
    return Optional.ofNullable(res.getResult()).orElse(false);
  }

  @Override
  @SneakyThrows
  public Map<String, JsonNode> resolvePlaceholders(Map<String, JsonNode> node, ScriptExecutionContext ctx) {
    ResolvePlaceholdersExecutionContext evalArgs = compileExecutionContext(node, ctx);
    var replaced = resolvePlaceholders(evalArgs).getResultNode();
    return OM.treeToValue(replaced, OM.getTypeFactory().constructMapLikeType(LinkedHashMap.class, String.class, JsonNode.class));
  }


  @Override
  public ScriptExecutionContext injectActivityArgsToContext(Map<String, JsonNode> args, ScriptExecutionContext ctx) {
    ResolvePlaceholdersExecutionContext evalArgs = compileExecutionContext(args, ctx);
    return scriptExecutorClient.inject(mapper.toResolvePlaceholdersExecutionContext(evalArgs));
  }

  @Override
  public JsonNode filter(Map<String, String> filter, ScriptExecutionContext ctx, JsonNode output) {
    ScriptExecutorClient.ReqFilterOutput
        request = new ScriptExecutorClient.ReqFilterOutput().setCtx(ctx).setFilter(filter).setOutput(output);
    return scriptExecutorClient.filterOutput(request).getResultNode();
  }

  @Override
  public JsonNode resolvePlaceholders(JsonNode node, ScriptExecutionContext ctx) {
    ResolvePlaceholdersExecutionContext evalArgs = new ResolvePlaceholdersExecutionContext();
    evalArgs.setNode(node);
    evalArgs.setScriptContext(ctx);
    return resolvePlaceholders(evalArgs).getResultNode();
  }

  @Override
  @SneakyThrows
  public <T> T resolvePlaceholders(T object, Class<T> clazz, ScriptExecutionContext ctx) {
    JsonNode node = OM.valueToTree(object);
    JsonNode resolved = resolvePlaceholders(node, ctx);
    return OM.treeToValue(resolved, clazz);
  }

}
