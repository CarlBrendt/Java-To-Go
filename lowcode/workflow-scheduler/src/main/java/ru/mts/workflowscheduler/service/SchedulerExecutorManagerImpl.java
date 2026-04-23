package ru.mts.workflowscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.controller.dto.Worker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerExecutorManagerImpl implements SchedulerExecutorManager {

  private final Set<UUID> allWorkers = ConcurrentHashMap.newKeySet();
  private final SchedulerExecutor schedulerExecutor;

  @Override
  public void startWorker(Worker worker) {
    allWorkers.add(worker.getId());
  }

  @Override
  public int getWorkerCount() {
    return allWorkers.size();
  }

  @Override
  public void startProcess() {
    allWorkers.forEach(schedulerExecutor::execute);
  }

  @Override
  public void stopStolenLocalWorkers(Set<UUID> workerIds) {
    allWorkers.removeIf(workerId -> !workerIds.contains(workerId));
  }

  @Override
  public Set<UUID> getWorkers() {
    return allWorkers;
  }

}
