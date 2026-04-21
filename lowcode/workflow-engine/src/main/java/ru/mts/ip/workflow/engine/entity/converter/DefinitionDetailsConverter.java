package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;

public class DefinitionDetailsConverter implements AttributeConverter<DefinitionDetails, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  @SneakyThrows
  public String convertToDatabaseColumn(DefinitionDetails attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsString(attribute);
  }

  @Override
  @SneakyThrows
  public DefinitionDetails convertToEntityAttribute(String dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, DefinitionDetails.class);
  }

}
