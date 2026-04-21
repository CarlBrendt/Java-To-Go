package ru.mts.ip.workflow.engine.esql;

import java.util.List;import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTaskState;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL) 
public class EsqlToLuaEvent {
  
  private String id = UUID.randomUUID().toString();
  private String type = "unknown";
  private EsqlToLuaTaskState currentTaskState;
  
  private List<ClientErrorDescription> errorDescriptions;
  private List<ErrorDescription> errors;
  private JsonNode workflowDefinition;

  
  public static String compileTopicName(String taskId) {
    return "esql-to-lua-%s".formatted(taskId);
  }
  
  public static EsqlToLuaEvent started() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.STARTED);
  }

  public static EsqlToLuaEvent luaScriptGenerationStarted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_GENERATION_STARTED);
  }

  public static EsqlToLuaEvent luaScriptGenerationCompleted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_GENERATION_COMPLETED);
  }

  public static EsqlToLuaEvent luaScriptTestingStarted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_TESTING_STARTED);
  }

  public static EsqlToLuaEvent luaScriptTestingCompleted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_TESTING_COMPLETED);
  }

  public static EsqlToLuaEvent luaScriptTuningStarted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_TUNING_STARTED);
  }

  public static EsqlToLuaEvent luaScriptTuningCompleted() {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.LUA_SCRIPT_TUNING_COMPLETED);
  }
  
  public static EsqlToLuaEvent completed(JsonNode result) {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.COMPLETED).setWorkflowDefinition(result);
  }

  public static EsqlToLuaEvent error(List<ClientErrorDescription> errors) {
    return new EsqlToLuaEvent().setType(Const.EsqlTaskEvent.ERROR).setErrorDescriptions(errors);
  }
  
}
