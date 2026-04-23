package ru.mts.workflowscheduler.engine;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowscheduler.controller.dto.Worker;
import ru.mts.workflowscheduler.entity.Ref;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowEngine {
  void startFlow(Ref definitionRef, String businessKey);
  void startFlow(Ref definitionRef, String businessKey, JsonNode vars, UUID workerId);

  Optional<Worker> getWorkerAndLock(UUID workerId);
  void startFlow(Ref definitionRef, String bk, UUID id);
}
