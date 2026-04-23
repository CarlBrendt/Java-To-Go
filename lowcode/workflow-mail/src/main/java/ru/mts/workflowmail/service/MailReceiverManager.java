package ru.mts.workflowmail.service;


import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.service.dto.ConsumerError;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MailReceiverManager {
  void startWorker(Worker starter);
  int getWorkerCount();
  void stopStolenLocalWorkers(Set<UUID> workerIds);

  List<ConsumerError> closeBrokenConsumers();

  Set<UUID> getHealthyConsumers();
}
