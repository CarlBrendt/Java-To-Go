package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.mts.workflowmail.service.dto.MailConsumerForInternal;
import ru.mts.workflowmail.share.script.ClientError;
import ru.mts.workflowmail.share.script.ClientErrorDescription;
import ru.mts.workflowmail.share.script.ScriptExecutionContext;

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

  @Override
  public Optional<JsonNode> transformMailOutput(JsonNode mailMessageOutput, JsonNode outputTemplate) {
    var template = Optional.ofNullable(outputTemplate)
        .filter(t -> {
          if(t.isNull()) {
            return false;
          } else {
            if(t.isObject() || t.isArray()) {
              if(t.isEmpty()) {
                return false;
              }
            }
          }
          return true;
        })
        .orElse(null);

    if(template != null) {
      template = scriptExecutorService.resolvePlaceholders(template, new ScriptExecutionContext(mailMessageOutput, null));
    }
    return Optional.ofNullable(template);
  }

  @Override
  public ScriptExecutionContext compileScriptContext(MailConsumerForInternal consumer) {
    var defaultVariables = (ObjectNode) createExampleForSchema(defaultValidationSchema).asNode();
    return new ScriptExecutionContext(defaultVariables);
  }

  private JsonExample createExampleForSchema(VariablesJsonSchema schema) {
    return new JsonExample(schema);
  }
}
