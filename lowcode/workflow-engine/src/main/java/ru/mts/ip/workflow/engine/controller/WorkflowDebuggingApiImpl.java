package ru.mts.ip.workflow.engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinitionSchema;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ResResolvePlaceholdersExecutionResult;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationService;

@RestController
@RequiredArgsConstructor
public class WorkflowDebuggingApiImpl implements WorkflowDebuggingApi {
  
  private final WorkflowExecutorService executor;
  private final ValidationService validationService;
  private final DtoMapper mapper;
  private final JsonSerializer serializer;
  private final ScriptExecutorService scriptExecutionService;

  @Override
  public ResponseEntity<String> debug(String expression) {
    return executor.mockRun(expression);
  }

  @Override
  public ResponseEntity<String> debugV2(String expression) {
    return executor.mockRunV2(expression);
  }

  @Override
  public ResponseEntity<ActivityExecutionContext> emulateActivityContext(String definition, String activityId) {
    var result = validationService.validateAndParseIgnoreErrors(definition, new ReqCreateWorkflowDefinitionSchema(Constraint.NOT_NULL)
        , ReqCreateWorkflowDefinition.class);
    ReqCreateWorkflowDefinition def = result.getParseResult();
    var validationResult = result.getValidationResult();
    if (def == null) {
      throw new ConstraintViolationException(validationResult.getErrors());
    } else { 
      WorkflowDefinition toDeploy = mapper.toWorkflowDefinition(def);
      DetailedWorkflowDefinition executable = serializer.toDetailedWorkflowDefinition(toDeploy);
      return ResponseEntity.ok(validationService.getActivityExecutionContext(executable, activityId));
    }
  }

  @Override
  public ResponseEntity<String> resolve(ReqResolvePlaceholdersExecutionContext request) {
    return scriptExecutionService.resolvePlaceholdersProxy(request);
  }

}
