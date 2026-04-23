package ru.mts.workflowmail.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import ru.mts.workflowmail.config.EngineConfigurationProperties;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.controller.dto.WorkerError;
import ru.mts.workflowmail.controller.dto.WorkerExtend;
import ru.mts.workflowmail.controller.dto.WorkerLocker;
import ru.mts.workflowmail.controller.dto.WorkerIdentity;
import ru.mts.workflowmail.engine.WorkflowEngineClient;
import ru.mts.workflowmail.service.dto.ConsumerError;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailWorkerLocalManagerImpl implements MailWorkerLocalManager {

  private final MailReceiverManager consumerManager;
  private final EngineConfigurationProperties props;
  private final WorkflowEngineClient engineClient;

  @Override
  public void tryInitialize() {
    if (consumerManager.getWorkerCount() < props.getMailWorkerLimitCount()) {
      try {
        Worker worker = null;
        try {
          worker = engineClient.lockWorker(WorkerLocker.defaultInstance());
        } catch (FeignException.NotFound ignored) {
          return;
        } catch (Exception e) {
          log.error("Can't lock Worker", e);
          return;
        }
        log.info("Try initialize worker: {}", worker.getId());
        var starter = worker.getStarter();
        MDC.put("starter-id", starter.getId().toString());
        MDC.put("workflow-ref-id", starter.getWorkflowDefinitionToStartId().toString());
        MDC.put("tenant-id", starter.getTenantId());
        try {
          consumerManager.startWorker(worker);
          engineClient.startWorker(new WorkerIdentity(worker.getId(), worker.getExecutorId()));
        } catch (Exception ex) {
          log.error("Scheduler initialization failed", ex);
          engineClient.errorWorker(
              WorkerError.fromThrowable(worker.getId(), ex));
        }
      } catch (Exception ex) {
        log.error("Unknown Scheduler starter initialization failed", ex);
      } finally {
        MDC.clear();
      }
      tryInitialize();
    }
  }

  @Override
  public void stopStolenLocalWorkers() {
    Set<UUID> workerIds = engineClient.getWorkerIds(Const.getApplicationInstanceId());
    consumerManager.stopStolenLocalWorkers(workerIds);
  }

  @Override
  public void logLocalState() {
    if (log.isWarnEnabled() && consumerManager.getWorkerCount() >= props.getMailWorkerLimitCount()){
      log.warn("Worker limit reached! Limit = {}, applicationInstanceId = {}", props.getMailWorkerLimitCount(), Const.getApplicationInstanceId());
    }
    log.info("Current WorkerCount = {}, applicationInstanceId = {}", consumerManager.getWorkerCount(), Const.getApplicationInstanceId());
  }

  @Override
  public void closeFailedConsumers() {
    var broken = consumerManager.closeBrokenConsumers();
    for (ConsumerError b : broken) {
      try {
        engineClient.errorWorker(
            WorkerError.fromThrowable(b.getWorkerId(), b.getError()));
      } catch (Exception ex) {
        log.error("Can't error worker {}", b.getWorkerId(), ex);
      }
    }
  }

  @Override
  public void doHeartbeat() {
    Set<UUID> healthyIds = consumerManager.getHealthyConsumers();
    if (!healthyIds.isEmpty()) {
      engineClient.extendWorker(new WorkerExtend(healthyIds, Const.getApplicationInstanceId()));
    }
  }


}
