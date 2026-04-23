package ru.mts.ip.workflow.engine.esql;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.dto.ScriptErrorContext;
import ru.mts.ip.workflow.engine.esql.EsqlLuaScriptProblemResolver.LuaScriptWithErrors;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTask;
import ru.mts.ip.workflow.engine.esql.EsqlService.SourceFile;
import ru.mts.ip.workflow.engine.esql.WorfklowDefinitionHelper.ActivityWrapper;
import ru.mts.ip.workflow.engine.esql.WorfklowDefinitionHelper.SwitchEntry;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlActivity;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlActivity.CompileTarget;
import ru.mts.ip.workflow.engine.esql.temporal.EsqlActivity.ScriptExecutionResult;
import ru.mts.ip.workflow.engine.esql.temporal.LuaActivity;
import ru.mts.ip.workflow.engine.esql.temporal.RedisActivity;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;
import ru.mts.ip.workflow.engine.llm.Utils;


public class WorkflowGenerator {

  private final WorfklowDefinitionHelper workflowHelper;

  private final EsqlActivity activity = Workflow.newActivityStub(EsqlActivity.class,
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10))
      .setTaskQueue("EsqlTaskQueue")
      .build());

  private final LuaActivity luaActivity = Workflow.newActivityStub(LuaActivity.class,
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10))
      .setTaskQueue("LuaTaskQueue")
      .build());

  private final RedisActivity redisActivity = Workflow.newActivityStub(RedisActivity.class,
      ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(10))
      .setTaskQueue("RedisTaskQueue")
      .build());

  public WorkflowGenerator(JsonNode def) {
    this.workflowHelper = new WorfklowDefinitionHelper(def);
  }

  public JsonNode generateNextFromSourceCode(EsqlToLuaTask task) {
    var injectParent = task.getParentActivityId();
    var lua = tryGenerateLua(task);
    addActivities(injectParent, task, lua);
    return workflowHelper.asNode();
  }
  
  
  String luaLabelConditionTemplate = """
  lua{
  function contains(list, value)
    for _, v in ipairs(list) do
      if v == value then
        return true
      end
    end
    return false
  end
  local result = esqlCompileResult and esqlCompileResult.propagates and contains(esqlCompileResult.propagates, '%s')
  return result == true
  }lua
  """;

  String luaDefaultFlowCondition = """
  lua{
  local result = OutputRoot and not(OutputRoot == '')
  return result == true
  }lua
  """;
  
  private void addActivities(String startActivityId, EsqlToLuaTask task, SourceFile lua) {
    String luaScript = lua.getContent();
    var luaExecutionInject = workflowHelper.createInject(Map.of("esqlCompileResult", "lua{%s}lua".formatted(luaScript)), "Lua");
    var finalActivityId = workflowHelper.findActivityById(startActivityId).flatMap(ActivityWrapper::getTransition).orElse(null);
    if(finalActivityId == null) {
      var exitInject = workflowHelper.createInject(Map.of("exit", "true"), "exit stub");
      finalActivityId = exitInject.getId();
    }
    workflowHelper.insertActivityAsNext(luaExecutionInject, startActivityId);
    var labels = Utils.findLabels(task.getEsqlSources());
    if(labels.isEmpty()) {
      luaExecutionInject.setTransition(finalActivityId);
    } else {
      List<ActivityWrapper> stubs = new ArrayList<>();
      labels.forEach(l -> { 
        var propagateInject = workflowHelper.createInject(Map.of(l, "executed"), "PROPAGATE TO LABEL %s".formatted(l));
        var propagateSwitch = workflowHelper.createSwitch(new SwitchEntry(), List.of(
          new SwitchEntry().setTransition(propagateInject.getId()).setCondition(luaLabelConditionTemplate.formatted(l))
        ));
        stubs.add(propagateSwitch);
      });
      var parall = workflowHelper.createParallel(stubs.stream().map(ActivityWrapper::getId).toList());
      parall.setTransition(finalActivityId);
      luaExecutionInject.setTransition(parall.getId());
    }
  }
  
  private SourceFile tryGenerateLua(EsqlToLuaTask compileTask) {
    onLuaGenerationStarted();
    int i = 3;
    var lua = activity.compileToLua(new CompileTarget().setSources(compileTask.getEsqlSources()));
    onLuaGenerationCompleted();
    var scriptContent = lua.getContent();
    var generatedData = luaActivity.generateTestData(lua);
    
    ScriptExecutionResult executionResult = null;
    while(!isExecutionSuccess(executionResult) && i > 0) {
      i--;
      onLuaTestingStarted();
      executionResult = tryExecute(scriptContent, generatedData.getVars());
      onLuaTestingCompleted();
      if(!isExecutionSuccess(executionResult)) {
        var errors = executionResult.getErrors();
        var result = executionResult.getResult();
        if(errors == null && (result == null || result.isNull())) {
          ScriptErrorContext scriptContext = new ScriptErrorContext();
          scriptContext.setSystemMessage("Результат скрипта должен вернуть lua таблицу");
          scriptContext.setVariableContext(generatedData.getVars());
          var ed = new ErrorDescription().setScriptContext(scriptContext);
          executionResult.setErrors(List.of(ed));
        }
        LuaScriptWithErrors luaWithErrors = new LuaScriptWithErrors()
            .setExecutionResult(executionResult)
            .setSrc(new SourceFile().setContent(scriptContent));
        onLuaTuningStarted();
        scriptContent = luaActivity.fixLuaScript(luaWithErrors).getContent();
        onLuaTuningCompleted();
      }
    }
    
    
    lua.setContent(scriptContent);
    return lua;
    
  }
  
  private void onLuaTuningCompleted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptTuningCompleted());
  }

  private void onLuaTuningStarted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptTuningStarted());
  }

  private void onLuaTestingStarted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptTestingStarted());
  }

  private void onLuaTestingCompleted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptTestingCompleted());
  }

  private void onLuaGenerationCompleted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptGenerationCompleted());
  }

  private void onLuaGenerationStarted() {
    redisActivity.sendEvent(Workflow.getInfo().getWorkflowId(), EsqlToLuaEvent.luaScriptGenerationStarted());
  }

  private boolean isExecutionSuccess(ScriptExecutionResult result) {
    return result != null && result.getResult() != null && !result.getResult().isNull();
  }
  

  ScriptExecutionResult tryExecute(String src, JsonNode context) {
    return activity.executeScript(src, context);
  }

  
}
