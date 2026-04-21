package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;
import ru.mts.ip.workflow.engine.json.JsonExample;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StarterScriptValidationServiceImpl implements StarterScriptValidationService{

  private final ObjectMapper objectMapper;
  private final ScriptExecutorService scriptExecutorService;
  

  @SneakyThrows
  private Optional<VariablesJsonSchema> toVariablesJsonSchema(JsonNode json) {
    return Optional.ofNullable(json).filter(node -> node.isObject() && !node.isEmpty()).map(this::parse);
  }
  
  @SneakyThrows
  private VariablesJsonSchema parse(JsonNode node) {
    return objectMapper.treeToValue(node, VariablesJsonSchema.class);
  }
  
  private Optional<JsonExample> creteExampleForSchema(JsonNode schema) {
    return toVariablesJsonSchema(schema).map(this::creteExampleForSchema);
  }

  private JsonExample creteExampleForSchema(VariablesJsonSchema schema) {
    return new JsonExample(schema);
  }

  @Override
  public Optional<ClientError> validateOutputTemplateValueReplacement(JsonNode value, ScriptExecutionContext ctx) {
    value = value.deepCopy();
    try {
      scriptExecutorService.resolvePlaceholders(value, ctx);
    } catch (ClientError ex) {
      return Optional.of(ex);
    }
    return Optional.empty();
  }
}
