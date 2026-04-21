package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import lombok.SneakyThrows;

public class FlowEditorConfigConverter implements AttributeConverter<JsonNode, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  @SneakyThrows
  public String convertToDatabaseColumn(JsonNode attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsString(attribute);
  }

  @Override
  @SneakyThrows
  public JsonNode convertToEntityAttribute(String dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readTree(dbData);
  }

}
