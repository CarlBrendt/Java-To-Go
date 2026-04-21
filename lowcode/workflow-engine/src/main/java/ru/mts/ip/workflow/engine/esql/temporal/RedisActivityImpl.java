package ru.mts.ip.workflow.engine.esql.temporal;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.esql.EsqlToLuaEvent;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;

@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "RedisTaskQueue")
public class RedisActivityImpl implements RedisActivity {

  private final RedisOperations<String, EsqlToLuaEvent> redis;
  private final ErrorCompiler errorCompiler;

  @Override
  @SneakyThrows
  public void sendEvent(String workflowId, EsqlToLuaEvent event) {
    if(event.getType().equals(Const.EsqlTaskEvent.ERROR)){
      var errorDescriptions = event.getErrorDescriptions();
      var compiledErrors = errorCompiler.toErrorDescription(errorDescriptions);
      event.setErrors(compiledErrors);
      event.setErrorDescriptions(null);
    }
    redis.convertAndSend(EsqlToLuaEvent.compileTopicName(workflowId), event);
  }

}
