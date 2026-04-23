package ru.mts.ip.workflow.engine.executor;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

@Data
@Accessors(chain = true)
public class WorkflowExpressionValidationResult {
  private List<ErrorDescription> errors;
}
