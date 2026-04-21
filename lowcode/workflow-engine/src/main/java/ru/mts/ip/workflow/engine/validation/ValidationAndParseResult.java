package ru.mts.ip.workflow.engine.validation;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ValidationAndParseResult<T> {
  private T parseResult;
  private ValidationResult validationResult;
}
