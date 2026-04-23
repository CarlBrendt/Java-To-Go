package ru.mts.workflowscheduler.service;


import ru.mts.workflowscheduler.controller.dto.Worker;

import java.util.Set;
import java.util.UUID;

public interface SchedulerExecutorManager {
  void startWorker(Worker worker);
  int getWorkerCount();

  void startProcess();


  void stopStolenLocalWorkers(Set<UUID> workerIds);

  Set<UUID> getWorkers();
}
