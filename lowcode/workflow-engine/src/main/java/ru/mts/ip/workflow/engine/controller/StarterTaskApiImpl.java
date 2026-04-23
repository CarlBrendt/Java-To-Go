package ru.mts.ip.workflow.engine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqStarterTask;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterTask;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;
import ru.mts.ip.workflow.engine.service.dto.StarterTask;
import ru.mts.ip.workflow.engine.service.starter.StarterTaskService;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StarterTaskApiImpl implements StarterTaskApi {
  private final StarterTaskService starterTaskService;
  private final DtoMapper mapper;

  @Override
  public void stopSapTask(UUID id) {
    starterTaskService.stopTask(id);
  }

  @Override
  public ResIdHolder save(ReqStarterTask req) {
    StarterTask starterTask = mapper.toStarterTask(req);
    StarterTaskEntity entity = starterTaskService.save(starterTask);
    return new ResIdHolder(entity.getId());
  }

  @Override
  public ResStarterTask restartSapTask(UUID id) {
    var entity = starterTaskService.restartTask(id);
    return mapper.toResStarterTask(entity);
  }

  @Override
  public ResStarterTask getSapTask(UUID id) {
    var entity = starterTaskService.getSapTask(id);
    return mapper.toResStarterTask(entity);
  }
}
