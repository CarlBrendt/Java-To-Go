package ru.mts.workflowscheduler.service;

import ru.mts.workflowscheduler.service.dto.CompatibilityStarter;
import ru.mts.workflowscheduler.share.validation.ValidationResult;
import ru.mts.workflowscheduler.share.validation.schema.BaseSchema;

public interface ValidationService {
  ValidationResult validate(String definition, BaseSchema schema);
  ValidationResult validateStarterCompatibility(CompatibilityStarter starter);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz);
  <T> T valid(String value, BaseSchema schema, Class<T> clazz, boolean ignoreWarnings);
}
