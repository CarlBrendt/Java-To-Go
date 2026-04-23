package ru.mts.ip.workflow.engine.esql;

import java.io.IOException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EsqlTaskEventListener implements MessageListener {

  private final SseEmitter emitter;
  private final ObjectMapper objectMapper;
  
  @Override
  public void onMessage(Message message, byte[] pattern) {
//    EsqlToLuaEvent event = serialize(message);
//    try {
//        emitter.send(SseEmitter.event().data(event).id(event.getId()).name(event.getType()));
//    }
//    catch (IOException ex) {
//        emitters.remove(emitter);
//    }
  }
  
  private EsqlToLuaEvent serialize(Message message) {
    try {
        return this.objectMapper.readValue(message.getBody(), EsqlToLuaEvent.class);
    }
    catch (IOException ex) {
        throw new RuntimeException(ex);
    }
}

}
