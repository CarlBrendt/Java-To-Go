package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.entity.KafkaStarterDetails;


@Converter(autoApply = true)
public class KafkaStarterDetailsConverter implements AttributeConverter<KafkaStarterDetails, byte[]> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @Override
  @SneakyThrows
  public byte[] convertToDatabaseColumn(KafkaStarterDetails attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsBytes(attribute);
  }

  @Override
  @SneakyThrows
  public KafkaStarterDetails convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, KafkaStarterDetails.class);
  }

}
