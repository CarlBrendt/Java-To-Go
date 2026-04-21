package ru.mts.ip.workflow.engine.esql.temporal;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

@ActivityInterface
public interface EsqlActivity {


  @ActivityMethod
  SourceFile compileToLua(CompileTarget data);

  @ActivityMethod
  ScriptExecutionResult executeScript(String script, JsonNode variables);
  
  @Data
  @Accessors(chain = true)
  public static class CompileTarget {
    private List<SourceFile> sources;
  }

  @Data
  @Accessors(chain = true)
  public static class CompileResult {
    private String luaScript;
  }
  
  @Data
  @Accessors(chain = true)
  public static class ScriptExecutionResult {
    private JsonNode result;
    private List<ErrorDescription> errors;
  }
  
}
