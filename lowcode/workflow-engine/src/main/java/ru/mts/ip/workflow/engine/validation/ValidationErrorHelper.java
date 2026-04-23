package ru.mts.ip.workflow.engine.validation;

import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorContext;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorLocation;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;

public interface ValidationErrorHelper {
  ErrorDescription compileError(Errors2 error, ErrorLocation location, ErrorContext context, ErrorMessageArgs messageArgs);
}