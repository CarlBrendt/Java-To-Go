package ru.mts.ip.workflow.engine.esql.temporal;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTask;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTaskState;
import ru.mts.ip.workflow.engine.esql.EsqlToLuaEvent;
import ru.mts.ip.workflow.engine.esql.WorkflowGenerator;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;

@WorkflowImpl(taskQueues = "EsqlToLuaTaskQueue")
public class WorkflowAssistantExecutor implements EsqlToLua {

  @SuppressWarnings("unused")
  private final Logger log = Workflow.getLogger(WorkflowAssistantExecutor.class);
  
  private final RedisActivity redisActivity = Workflow.newActivityStub(RedisActivity.class,
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10))
      .setTaskQueue("RedisTaskQueue")
      .build());
  
  private JsonNode definition;
  private String currentStatus = Const.EsqlTaskStatus.IN_PROGRESS;
  private List<ClientErrorDescription> errors = List.of();
  
  @Override
  public void start(EsqlToLuaTask initialData) {
    onStarted(initialData);
    WorkflowGenerator generator = new WorkflowGenerator(definition);
    try {
      onCompleted(generator.generateNextFromSourceCode(initialData));
    } catch (ClientError err) {
      onError(err);
    } catch (Exception ex) {
      onError(new ClientError(Errors2.UNEXPECTED, new ErrorMessagePouch().setSystemMessage(ex.getMessage())));
    }
    
  }
  
  private void onError(ClientError err) {
    currentStatus = Const.EsqlTaskStatus.ERROR;
    errors = err.getErrors();
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.error(errors));
  }

  private void onCompleted(JsonNode result) {
    definition = result;
    currentStatus = Const.EsqlTaskStatus.COMPLETED;
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.completed(result));
  }

  private void onStarted(EsqlToLuaTask task) {
    definition = task.getWorkflowDefinition();
    currentStatus = Const.EsqlTaskStatus.STARTED;
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.started());
  }
  

  @Override
  public EsqlToLuaTaskState getState() {
    return new EsqlToLuaTaskState()
      .setStatus(currentStatus)
      .setRawErrors(errors)
      .setWorkflowDefinition(definition);
  }

}
