package ru.mts.ip.workflow.engine.controller;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqWorkerReplace;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerError;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerExtend;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerIdentity;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerLocker;
import ru.mts.ip.workflow.engine.service.starter.WorkerService;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.v1.StarterWorkerSchema;

@RestController
@RequiredArgsConstructor
public class WorkerApiImpl implements WorkerApi {
  private final WorkerService workerService;
  private final ValidationService validationService;

  @Override
  public ResponseEntity<ResWorker> getWorkerAndLock(WorkerLocker locker) {
    return workerService.getExecutableWorkerAndLock(locker)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public void startWorker(WorkerIdentity start) {
    workerService.startWorker(start);
  }

  @Override
  public void errorWorker(WorkerError error) {
    workerService.errorWorker(error);
  }

  @Override
  public void extendWorker(WorkerExtend extend) {
    workerService.extendWorker(extend);
  }

  @Override
  public void deleteWorker(UUID id) {
    workerService.deleteWorker(id);
  }

  @Override
  public ResponseEntity<ResWorker> getWorker(UUID id) {
    return workerService.getWorker(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public void replaceWorker(UUID id, String value) {
    ReqWorkerReplace req = validationService.valid(value, new StarterWorkerSchema(Constraint.NOT_NULL)
        , ReqWorkerReplace.class);
    workerService.replaceWorker(id, req);
  }

  @Override
  public Set<UUID> getWorkerIds(UUID executorId) {
    return workerService.getWorkerIds(executorId);
  }
}
