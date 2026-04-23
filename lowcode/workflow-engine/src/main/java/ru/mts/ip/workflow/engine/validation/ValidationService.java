package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.controller.ActivityExecutionContext;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;

public interface ValidationService {
  ValidationResult validate(String definition, BaseSchema schema);
  ValidationResult validate(JsonNode json, BaseSchema schema);
  <T> ValidationAndParseResult<T> validateAndParse(String value, BaseSchema schema, Class<T> clazz);
  <T> ValidationAndParseResult<T> validateAndParseIgnoreErrors(String value, BaseSchema schema, Class<T> clazz);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz, boolean ignoreWarnings);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz);
  ValidationResult validateRuntime(DetailedWorkflowDefinition executable);
  ActivityExecutionContext getActivityExecutionContext(DetailedWorkflowDefinition executable, String activityId);
}
