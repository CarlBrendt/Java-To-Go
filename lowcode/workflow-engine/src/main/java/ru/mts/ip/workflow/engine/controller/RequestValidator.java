package ru.mts.ip.workflow.engine.controller;

import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowExpressionValidationResult;

public interface RequestValidator {
  ResWorkflowExpressionValidationResult validateWorkflowDefinition(String potentialDefinition);
  ResWorkflowExpressionValidationResult validateWorkflowExpression(String potentialExpression);
}
