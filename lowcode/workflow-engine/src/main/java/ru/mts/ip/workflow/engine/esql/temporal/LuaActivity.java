package ru.mts.ip.workflow.engine.esql.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import ru.mts.ip.workflow.engine.esql.EsqlLuaScriptProblemResolver.LuaScriptWithErrors;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.esql.LuaTestDataGenerator.LuaTestContext;

@ActivityInterface
public interface LuaActivity {

  @ActivityMethod
  LuaTestContext generateTestData(SourceFile file);
  
  @ActivityMethod
  SourceFile fixLuaScript(LuaScriptWithErrors script);
  
}
