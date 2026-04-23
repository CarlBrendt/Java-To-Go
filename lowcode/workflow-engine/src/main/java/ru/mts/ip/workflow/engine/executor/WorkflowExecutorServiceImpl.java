package ru.mts.ip.workflow.engine.executor;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import feign.Response;
import feign.codec.StringDecoder;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.ActivityExecutionContext;
import ru.mts.ip.workflow.engine.controller.RuntimeWorkflowExpression;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;

@Service
@RequiredArgsConstructor
public class WorkflowExecutorServiceImpl implements WorkflowExecutorService{

  private final WorkflowExecutorServiceClient client;
  private final StringDecoder stringDecoder = new StringDecoder();
  
  @Override
  public WorkflowExpressionValidationResult runtimeValidateExpression(RuntimeWorkflowExpression expression) {
    return client.runtimeValidate(expression);
  }

  @Override
  public WorkflowExpressionValidationResult validateExpression(String potentialExpression) {
    return client.validate(potentialExpression);
  }

  @Override
  public WorkflowExpressionValidationResult validateExpression(JsonNode potentialExpression) {
    return client.validate(potentialExpression);
  }

  @Override
  public ResponseEntity<String> mockRun(String potentialExpression) {
    return toResponseEntity(client.mockRun(potentialExpression));
  }
  
  private ResponseEntity<String> toResponseEntity(Response response){
    try {
      String str = (String) stringDecoder.decode(response, String.class);
      return new ResponseEntity<>(str, HttpStatus.valueOf(response.status()));
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public ExternalProperties resolvePropperties(ResolveExternalPropertiesConfig confing) {
    return client.resolve(confing);
  }

  @Override
  public WorkflowExpressionValidationResult validateActivity(JsonNode potentialActivity) {
    return client.validateActivity(potentialActivity);
  }

  @Override
  public ResponseEntity<String> mockRunV2(String potentialExpression) {
    return toResponseEntity(client.mockRunV2(potentialExpression));
  }

  @Override
  public JsonNode applyExpressionDefualts(JsonNode compiled) {
    return client.applyDefaults(compiled);
  }

  @Override
  public InstanceHistory evaluateHistory(InstanceHistory hist) {
    return client.evaluateHistory(hist);
  }

  @Override
  public JsonNode filterCompiled(JsonNode compiled) {
    return compiled;
  }

  @Override
  public ActivityExecutionContext emulateActivityContext(RuntimeWorkflowExpression expression, String activityId) {
    return client.emulateActivityContext(expression, activityId);
  }

  @Override
  public WorkflowExpressionValidationResult validateExpressionForEsqlCompilation(JsonNode potentialExpression) {
    return client.validateForEsqlCompilation(potentialExpression);
  }
  
}
