package ru.mts.ip.workflow.engine.executor;

import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.controller.ActivityExecutionContext;
import ru.mts.ip.workflow.engine.controller.RuntimeWorkflowExpression;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;

public interface WorkflowExecutorService {
  ActivityExecutionContext emulateActivityContext(RuntimeWorkflowExpression expression, String activityId);
  WorkflowExpressionValidationResult runtimeValidateExpression(RuntimeWorkflowExpression expression);
  WorkflowExpressionValidationResult validateExpression(String potentialExpression);
  WorkflowExpressionValidationResult validateExpression(JsonNode potentialExpression);
  WorkflowExpressionValidationResult validateExpressionForEsqlCompilation(JsonNode potentialExpression);
  WorkflowExpressionValidationResult validateActivity(JsonNode potentialActivity);
  JsonNode filterCompiled(JsonNode compiled);
  InstanceHistory evaluateHistory(InstanceHistory hist);
  ResponseEntity<String> mockRun(String potentialExpression);
  ResponseEntity<String> mockRunV2(String potentialExpression);
  ExternalProperties resolvePropperties(ResolveExternalPropertiesConfig confing);
  JsonNode applyExpressionDefualts(JsonNode expression);
}
