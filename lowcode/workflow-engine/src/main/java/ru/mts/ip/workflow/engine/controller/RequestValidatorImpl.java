package ru.mts.ip.workflow.engine.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinitionSchema;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowExpressionValidationResult;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationResult;
import ru.mts.ip.workflow.engine.validation.ValidationService;

@Service
@RequiredArgsConstructor
public class RequestValidatorImpl implements RequestValidator {
  
  private final ValidationService validationService;
  private final DtoMapper mapper;
  private final JsonSerializer serializer;
  private final WorkflowExecutorService executor;

  @Override
  public ResWorkflowExpressionValidationResult validateWorkflowExpression(String potentialExpression) {
    return mapper.toResWorkflowExpressionValidationResult(executor.validateExpression(potentialExpression));
    
  }

  @Override
  public ResWorkflowExpressionValidationResult validateWorkflowDefinition(String request) {
    List<ErrorDescription> allErrors = new ArrayList<>();
    var resultWithParsed = validationService.validateAndParse(request, new ReqCreateWorkflowDefinitionSchema(Constraint.NOT_NULL),
        ReqCreateWorkflowDefinition.class);
    var validationResult = resultWithParsed.getValidationResult();
    WorkflowDefinition definition = mapper.toWorkflowDefinition(resultWithParsed.getParseResult());
    allErrors.addAll(validationResult.getErrors());
    if(!validationResult.containCriticalErrors()) {
      DetailedWorkflowDefinition detailed = serializer.toDetailedWorkflowDefinition(definition);
      ValidationResult runtimeValidationResult = validationService.validateRuntime(detailed);
      allErrors.addAll(runtimeValidationResult.getErrors());
    }
    return new ResWorkflowExpressionValidationResult().setErrors(mapper.toResErrorDescriptions(allErrors));
  }
  

}
