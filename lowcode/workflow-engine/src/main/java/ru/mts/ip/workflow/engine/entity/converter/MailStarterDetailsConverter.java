package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.entity.MailStarterDetails;
import ru.mts.ip.workflow.engine.entity.RabbitmqStarterDetails;


@Converter(autoApply = true)
public class MailStarterDetailsConverter implements AttributeConverter<MailStarterDetails, byte[]> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .registerModule(new JavaTimeModule());

  @Override
  @SneakyThrows
  public byte[] convertToDatabaseColumn(MailStarterDetails attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsBytes(attribute);
  }

  @Override
  @SneakyThrows
  public MailStarterDetails convertToEntityAttribute(byte[] dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, MailStarterDetails.class);
  }

}
