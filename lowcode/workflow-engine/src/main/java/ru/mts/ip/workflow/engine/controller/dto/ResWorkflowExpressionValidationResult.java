package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResErrorDescription;

@Data
@Accessors(chain = true)
public class ResWorkflowExpressionValidationResult {
  private List<ResErrorDescription> errors;
}
