package ru.mts.workflowscheduler.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.mts.workflowscheduler.config.EngineConfigurationProperties;
import ru.mts.workflowscheduler.controller.dto.Worker;
import ru.mts.workflowscheduler.controller.dto.WorkerError;
import ru.mts.workflowscheduler.controller.dto.WorkerExtend;
import ru.mts.workflowscheduler.controller.dto.WorkerLocker;
import ru.mts.workflowscheduler.controller.dto.WorkerIdentity;
import ru.mts.workflowscheduler.engine.WorkflowEngineClient;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerWorkerLocalManagerImpl implements SchedulerWorkerLocalManager {

  private final SchedulerExecutorManager executor;
  private final EngineConfigurationProperties props;
  private final WorkflowEngineClient engineClient;

  @Override
  public void tryInitialize() {
    if (executor.getWorkerCount() < props.getSchedulerWorkerLimitCount()) {

      getOverdueTaskAndLock().ifPresent(worker -> {
        try {
          log.info("Try initialize worker: {}", worker.getId());
          executor.startWorker(worker);
          engineClient.startWorker(new WorkerIdentity(worker.getId(), worker.getExecutorId()));
        } catch (Exception ex) {
          log.error("Scheduler initialization failed", ex);
          engineClient.errorWorker(
              new WorkerError(worker.getId(), worker.getExecutorId(), ex.getMessage()));
        }
      });
    }
  }

  private Optional<Worker> getOverdueTaskAndLock() {
    try {
      return Optional.of(engineClient.lockWorker(WorkerLocker.defaultInstance()));
    } catch (FeignException.NotFound ignored) {
    }
    return Optional.empty();
  }

  @Override
  public void stopStolenLocalWorkers() {
    Set<UUID> workerIds = engineClient.getWorkerIds(Const.getApplicationInstanceId());
    executor.stopStolenLocalWorkers(workerIds);
  }

  @Override
  public void logLocalState() {
  }

  @Override
  public void startProcess() {
    executor.startProcess();
  }

  @Override
  public void doHeartbeat() {
    Set<UUID> healthyIds = executor.getWorkers();
    if (!healthyIds.isEmpty()) {
      engineClient.extendWorker(new WorkerExtend(healthyIds, Const.getApplicationInstanceId()));
    }
  }

}
