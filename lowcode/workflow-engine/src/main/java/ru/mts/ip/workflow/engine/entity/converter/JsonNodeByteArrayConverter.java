package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;


@Converter(autoApply = true)
public class JsonNodeByteArrayConverter implements AttributeConverter<JsonNode, byte[]> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  @SneakyThrows
  public byte[] convertToDatabaseColumn(JsonNode attribute) {
    return attribute == null || attribute.isNull() ? null : OBJECT_MAPPER.writeValueAsBytes(attribute);
  }

  @Override
  @SneakyThrows
  public JsonNode convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readTree(dbData);
  }

}
