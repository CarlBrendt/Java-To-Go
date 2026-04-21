package ru.mts.ip.workflow.engine.esql.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import ru.mts.ip.workflow.engine.esql.EsqlToLuaEvent;

@ActivityInterface
public interface RedisActivity {

  @ActivityMethod
  void sendEvent(String workflowId, EsqlToLuaEvent event);

}
