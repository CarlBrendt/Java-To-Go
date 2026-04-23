package ru.mts.workflowmail.service;

import ru.mts.workflowmail.service.dto.CompatibilityStarter;
import ru.mts.workflowmail.share.validation.ValidationResult;
import ru.mts.workflowmail.share.validation.schema.BaseSchema;

public interface ValidationService {
  ValidationResult validate(String definition, BaseSchema schema);
  ValidationResult validateStarterCompatibility(CompatibilityStarter starter);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz, boolean ignoreWarnings);
}
