package ru.mts.ip.workflow.engine.controller;

import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowExpressionValidationResult;

@RestController
@RequiredArgsConstructor
public class WorkflowValidationApiImpl implements WorkflowValidationApi{
  
  private final RequestValidator requestValidator;
  
  @Override
  public ResWorkflowExpressionValidationResult validate(String potentialExpression) {
    return requestValidator.validateWorkflowExpression(potentialExpression);
  }

  @Override
  public ResWorkflowExpressionValidationResult validateDefinition(String potentianlDefinition) {
    return requestValidator.validateWorkflowDefinition(potentianlDefinition);
  }

}
