package ru.mts.ip.workflow.engine.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ResEsqlToLuaTaskState;
import ru.mts.ip.workflow.engine.esql.EsqlService;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTask;
import ru.mts.ip.workflow.engine.esql.SseEmitterManager;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.v1.esql.EsqlToLuaTaskSchema;

@RestController
@RequiredArgsConstructor
public class EsqlApiImpl implements EsqlApi {
  
  private final EsqlService esqlService;
  private final DtoMapper mapper;
  private final ValidationService validationService;
  private final SseEmitterManager sseEmitterManager;
  
  public ResEsqlToLuaTaskState createCompilationTask(String task) {
    EsqlToLuaTask req = validationService.valid(task, new EsqlToLuaTaskSchema(Constraint.NOT_NULL), EsqlToLuaTask.class);
    var res = esqlService.createCompilationTask(req);
    return mapper.toResEsqlToLuaTaskState(res);
  }

  @Override
  public ResEsqlToLuaTaskState getCompilationTaskState(String id) {
    var res = esqlService.getCompilationTaskState(id);
    return mapper.toResEsqlToLuaTaskState(res);
  }

  @Override
  public SseEmitter sse(String task) {
    EsqlToLuaTask req = validationService.valid(task, new EsqlToLuaTaskSchema(Constraint.NOT_NULL), EsqlToLuaTask.class);
    return sseEmitterManager.createAndsubscribeToEsqlTask(req);
  }

}
