package ru.mts.workflowmail.service;

public interface MailWorkerLocalManager {
  void tryInitialize();
  void stopStolenLocalWorkers();
  void logLocalState();

  void closeFailedConsumers();

  void doHeartbeat();
}
