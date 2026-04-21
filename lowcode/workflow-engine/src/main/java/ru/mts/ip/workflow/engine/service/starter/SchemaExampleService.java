package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.json.JsonExample;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;

import java.util.Optional;

public interface SchemaExampleService {
  Optional<JsonExample> createExampleForSchema(JsonNode schema);

  JsonExample createExampleForSchema(VariablesJsonSchema kafkaDefaultValidationSchema);
}
