package ru.mts.ip.workflow.engine.esql.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTask;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTaskState;

@WorkflowInterface
public interface EsqlToLua {
  
  @WorkflowMethod
  void start(EsqlToLuaTask compilationTask);
  
  @QueryMethod
  EsqlToLuaTaskState getState();

}
