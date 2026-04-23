package ru.mts.workflowscheduler.service;

public interface SchedulerWorkerLocalManager {
  void tryInitialize();
  void stopStolenLocalWorkers();
  void logLocalState();

  void startProcess();

  void doHeartbeat();
}
