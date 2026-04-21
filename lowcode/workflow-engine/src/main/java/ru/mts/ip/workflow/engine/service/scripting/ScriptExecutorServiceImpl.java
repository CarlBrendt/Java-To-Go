package ru.mts.ip.workflow.engine.service.scripting;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.StringDecoder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqFilterOutput;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqIsExecutable;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ResIsExecutable;

@Service
@RequiredArgsConstructor
public class ScriptExecutorServiceImpl implements ScriptExecutorService{

  private final ScriptExecutorClient scriptExecutorClient;
  private final DtoMapper mapper;
  private final static ObjectMapper OM = new ObjectMapper();
  private final StringDecoder stringDecoder = new StringDecoder();
  
  
  @Override
  public ResolvePlaceholdersExecutionResult resolvePlaceholders(ResolvePlaceholdersExecutionContext ctx) {
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
    ResIsExecutable res = scriptExecutorClient.isExecutable(new ReqIsExecutable().setScript(script));
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
    ReqFilterOutput request = new ReqFilterOutput().setCtx(ctx).setFilter(filter).setOutput(output);
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
  
  private ResponseEntity<String> toResponseEntity(Response response){
    try {
      String str = (String) stringDecoder.decode(response, String.class);
      return new ResponseEntity<>(str, HttpStatus.valueOf(response.status()));
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public ResponseEntity<String> resolvePlaceholdersProxy(ReqResolvePlaceholdersExecutionContext request) {
    return toResponseEntity(scriptExecutorClient.resolvePlaceholdersProxy(mapper.toReqResolvePlaceholdersExecutionContext(request)));
  }

}
