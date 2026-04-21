package ru.mts.ip.workflow.engine.esql;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTask;

@Service
@RequiredArgsConstructor
public class SseEmitterManager {

  private final RedisMessageListenerContainer redisMessageListenerContainer;
  private final ObjectMapper objectMapper;
  private final EsqlService esqlService;
  private Map<UUID, List<SseEmitter>> subsByTask = new ConcurrentHashMap<>();

  @SneakyThrows
  public SseEmitter createAndsubscribeToEsqlTask(EsqlToLuaTask task) {
    var taskId = task.getTaskId();
    var emitters = subsByTask.getOrDefault(taskId, new CopyOnWriteArrayList<SseEmitter>());
    subsByTask.put(taskId, emitters);
    var emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(10));

    emitter.onCompletion(() -> {
      emitters.remove(emitter);
    });

    emitter.onTimeout(() -> {
      emitters.remove(emitter);
    });
    
    emitter.onError(err -> {
      emitters.remove(emitter);
    });

    emitters.add(emitter);

    MessageListener messageListener = (message, pattern) -> {
      EsqlToLuaEvent event = serialize(message);
      try {
        emitter.send(SseEmitter.event().data(event).id(event.getId()).name(event.getType()));
      } catch (IOException ex) {
        emitter.completeWithError(ex);
      }
    };


    redisMessageListenerContainer.addMessageListener(messageListener,
        ChannelTopic.of(getChannelName(taskId.toString())));
    emitter.onCompletion(() -> {
      emitters.remove(emitter);
      this.redisMessageListenerContainer.removeMessageListener(messageListener);
    });
    
    esqlService.createCompilationTask(task);

    return emitter;
  }

  private String getChannelName(String taskId) {
    return EsqlToLuaEvent.compileTopicName(taskId);
  }

  private EsqlToLuaEvent serialize(Message message) {
    try {
      return this.objectMapper.readValue(message.getBody(), EsqlToLuaEvent.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  void pingClient() {
    for(var emmiters : subsByTask.values()) {
      var it = emmiters.iterator();
      while(it.hasNext()) {
        var next = it.next();
        try {
          next.send(SseEmitter.event().name("ping"));
        } catch (IOException e) {
          next.complete();
          it.remove();
        }
      }
    }
  }
  
}
