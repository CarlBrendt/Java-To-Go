package ru.mts.ip.workflow.engine.executor;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.fasterxml.jackson.databind.JsonNode;
import feign.Response;
import ru.mts.ip.workflow.engine.controller.ActivityExecutionContext;
import ru.mts.ip.workflow.engine.controller.RuntimeWorkflowExpression;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;

@FeignClient(value = "wf-executor-client")
public interface WorkflowExecutorServiceClient {

  @PostMapping(value = "/api/v1/wf/expression/validate", produces = "application/json")
  WorkflowExpressionValidationResult validate(JsonNode potentialExpression);

  @PostMapping(value = "/api/v1/wf/expression/validate-for-esql-compilation", produces = "application/json")
  WorkflowExpressionValidationResult validateForEsqlCompilation(JsonNode potentialExpression);

  @PostMapping(value = "/api/v1/wf/activity/validate", produces = "application/json")
  WorkflowExpressionValidationResult validateActivity(JsonNode potentialActivity);

  @PostMapping(value = "/api/v1/wf/expression/validate", produces = "application/json")
  WorkflowExpressionValidationResult validate(@RequestBody(required = false) String potentialExpression);

  @PostMapping(value = "/api/v1/wf/expression/runtime-validate", produces = "application/json")
  WorkflowExpressionValidationResult runtimeValidate(@RequestBody RuntimeWorkflowExpression expression);

  @PostMapping(value = "/api/v1/wf/mocked-expression/run", produces = "application/json")
  Response mockRun(@RequestBody(required = false) String potentialExpression);

  @PostMapping(value = "/api/v2/wf/mocked-expression/run", produces = "application/json")
  Response mockRunV2(@RequestBody(required = false) String potentialExpression);

  @PostMapping(value = "/api/v1/wf/external-properties/resolve", produces = "application/json")
  ExternalProperties resolve(@RequestBody(required = false) ResolveExternalPropertiesConfig request);

  @PostMapping(value = "/api/v1/wf/expression/apply-defaults", produces = "application/json")
  JsonNode applyDefaults(@RequestBody JsonNode expression);

  @PostMapping(value = "/api/v1/wf/history/evaluate", produces = "application/json")
  InstanceHistory evaluateHistory(@RequestBody InstanceHistory history);
  
  @PostMapping(value = "/api/v1/wf/expression/emulate-activity-context/{activityId}", produces = "application/json")
  ActivityExecutionContext emulateActivityContext(@RequestBody RuntimeWorkflowExpression expression, @PathVariable String activityId);
  
}
