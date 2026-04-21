package ru.mts.ip.workflow.engine.entity.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression;

public class WorkflowExpressionConverter implements AttributeConverter<WorkflowExpression, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  @SneakyThrows
  public String convertToDatabaseColumn(WorkflowExpression attribute) {
    return attribute == null ? null : OBJECT_MAPPER.writeValueAsString(attribute);
  }

  @Override
  @SneakyThrows
  public WorkflowExpression convertToEntityAttribute(String dbData) {
    return dbData == null ? null : OBJECT_MAPPER.readValue(dbData, WorkflowExpression.class);
  }

}
