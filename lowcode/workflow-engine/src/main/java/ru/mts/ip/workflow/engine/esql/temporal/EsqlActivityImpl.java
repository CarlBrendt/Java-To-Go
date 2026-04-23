package ru.mts.ip.workflow.engine.esql.temporal;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.esql.EsqlComputeModuleToLuaCompiler;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;

@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "EsqlTaskQueue")
public class EsqlActivityImpl implements EsqlActivity{

  private final EsqlComputeModuleToLuaCompiler computeModuleToLuaCompiler;
  private final ScriptExecutorService scriptExecutorService;

  @Override
  public SourceFile compileToLua(CompileTarget data) {
    return computeModuleToLuaCompiler.compileToLua(data);
  }
  
  @Override
  public ScriptExecutionResult executeScript(String script, JsonNode variables) {
    ScriptExecutionResult res = new ScriptExecutionResult();
    ScriptExecutionContext ctx = new ScriptExecutionContext(variables);
    try {
      var node = scriptExecutorService.executeScript("lua{%s}lua".formatted(script), ctx);
      res.setResult(node);
    } catch (ClientError er) {
      res.setErrors(er.getCompiledErrors());
    }
    return res;
  }


}
