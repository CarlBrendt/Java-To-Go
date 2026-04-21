package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.json.JsonExample;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchemaExampleServiceImpl implements SchemaExampleService {
  private final ObjectMapper objectMapper;

  @Override
  public Optional<JsonExample> createExampleForSchema(JsonNode schema) {
    return toVariablesJsonSchema(schema).map(this::createExampleForSchema);
  }

  @Override
  public JsonExample createExampleForSchema(VariablesJsonSchema schema) {
    return new JsonExample(schema);
  }

  @SneakyThrows
  private Optional<VariablesJsonSchema> toVariablesJsonSchema(JsonNode json) {
    return Optional.ofNullable(json).filter(node -> node.isObject() && !node.isEmpty()).map(this::parse);
  }

  @SneakyThrows
  private VariablesJsonSchema parse(JsonNode node) {
    var a = objectMapper.treeToValue(node, VariablesJsonSchema.class);
    return a;
  }
}
