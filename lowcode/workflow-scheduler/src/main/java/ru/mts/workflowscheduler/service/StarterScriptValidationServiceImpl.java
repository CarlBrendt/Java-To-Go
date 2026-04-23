package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.share.script.ClientError;
import ru.mts.workflowscheduler.share.script.ClientErrorDescription;
import ru.mts.workflowscheduler.share.script.ScriptExecutionContext;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StarterScriptValidationServiceImpl implements StarterScriptValidationService{

  private final ObjectMapper objectMapper;
  private final ErrorCompiler errorCompiler;
  private final VariablesJsonSchema defaultValidationSchema;
  private final JsonValidator variableValidator;
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


  @Override
  public List<ClientErrorDescription> validateJson(JsonNode output, JsonNode validateSchema) {
    return variableValidator.validateVariables(output, validateSchema);
  }


}
