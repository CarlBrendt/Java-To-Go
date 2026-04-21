package ru.mts.ip.workflow.engine.service.starter;

import ru.mts.ip.workflow.engine.controller.dto.starter.ReqWorkerReplace;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerError;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerExtend;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerIdentity;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerLocker;
import ru.mts.ip.workflow.engine.exception.ClientError;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkerService {
  Optional<ResWorker> getExecutableWorkerAndLock(WorkerLocker locker);

  void startWorker(WorkerIdentity w);

  void errorWorker(WorkerError error);

  void extendWorker(WorkerExtend extend);

  void deleteWorker(UUID id);

  void replaceWorker(UUID id, ReqWorkerReplace worker);

  Set<UUID> getWorkerIds(UUID executorId);

  Optional<ResWorker> getWorker(UUID id);

  void successWorker(UUID workerID);

  void checkWorkerExists(WorkerIdentity workerIdentity) throws ClientError;
}
