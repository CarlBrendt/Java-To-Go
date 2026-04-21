package ru.mts.ip.workflow.engine.esql.temporal;

import org.springframework.stereotype.Component;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.esql.EsqlLuaScriptProblemResolver;
import ru.mts.ip.workflow.engine.esql.EsqlLuaScriptProblemResolver.LuaScriptWithErrors;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.esql.LuaTestDataGenerator;
import ru.mts.ip.workflow.engine.esql.LuaTestDataGenerator.LuaTestContext;

@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "LuaTaskQueue")
public class LuaActivityImpl implements LuaActivity{

  private final LuaTestDataGenerator luaTestDataGenerator;
  private final EsqlLuaScriptProblemResolver luaScriptProblemResolver;
    
  @Override
  public LuaTestContext generateTestData(SourceFile file) {
    return luaTestDataGenerator.generateTestData(file);
  }

  @Override
  public SourceFile fixLuaScript(LuaScriptWithErrors script) {
    return luaScriptProblemResolver.fixLuaScript(script);
  }

}
