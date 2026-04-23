package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.entity.SapTaskDetails;

@Converter
public class SapTaskDetailsConverter implements AttributeConverter<SapTaskDetails, byte[]> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @Override
  @SneakyThrows
  public byte[] convertToDatabaseColumn(SapTaskDetails attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsBytes(attribute);
  }

  @Override
  @SneakyThrows
  public SapTaskDetails convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, SapTaskDetails.class);
  }

}
