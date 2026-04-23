package ru.mts.ip.workflow.engine.esql;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlToLua;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;

@Service
@RequiredArgsConstructor
public class EsqlServiceImpl implements EsqlService{

  private final WorkflowClient workflowClient;
  private final ErrorCompiler errorCompiler;
  

  @Override
  public EsqlToLuaTaskState createCompilationTask(EsqlToLuaTask task) {
    String workflowId = task.getTaskId().toString();
    var options = WorkflowOptions.newBuilder().setTaskQueue("EsqlToLuaTaskQueue").setWorkflowId(workflowId).build();
    workflowClient.newUntypedWorkflowStub("EsqlToLua", options).start(task);
    return getCompilationTaskState(workflowId).setTaskId(workflowId);
  }

  @Override
  public EsqlToLuaTaskState getCompilationTaskState(String id) {
    EsqlToLua workflow = workflowClient.newWorkflowStub(EsqlToLua.class, id);
    var state = workflow.getState();
    var descriptions = errorCompiler.toErrorDescription(state.getRawErrors());
    state.setErrors(descriptions);
    state.setRawErrors(null);
    return state;
  }

  @Scheduled(fixedRate = 5_000)
  void handleSubs() {
    subsByTask.forEach((task, subs) -> {
      if(!subs.isEmpty()) {
        var taskState = getCompilationTaskState(task);
        sendState(taskState, subs);
      }
    });
  }
  
  private Map<String, List<SseEmitter>> subsByTask = new ConcurrentHashMap<>();
  private final RedisMessageListenerContainer redisMessageListenerContainer;
  
//  @Override
  public SseEmitter subscribeToTaskEvents(String id) {
//    var taskState = getCompilationTaskState(id);
    var taskSubs = subsByTask.getOrDefault(id, new CopyOnWriteArrayList<SseEmitter>());
    subsByTask.put(id, taskSubs);
    var res = new SseEmitter(300_000L);
   
    res.onCompletion(() -> {
      taskSubs.remove(res);
    });
    
    res.onTimeout(() -> {
      taskSubs.remove(res);
    });
    
//    sendState(taskState, List.of(res));
    taskSubs.add(res);
    
    
    
    return res;
  }
  
  private void sendState (EsqlToLuaTaskState state, List<SseEmitter> subs) {
    for(var s : subs) {
      try {
        s.send(SseEmitter.event()
            .id("task_state")
            .name("task")
            .data(state));
      } catch (IOException ex) {
        s.completeWithError(ex);
      }
    }
  }

}
