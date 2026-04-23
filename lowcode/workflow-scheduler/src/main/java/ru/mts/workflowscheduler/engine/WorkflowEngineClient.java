package ru.mts.workflowscheduler.engine;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mts.workflowscheduler.controller.dto.ReqWorkflowForStarter;
import ru.mts.workflowscheduler.controller.dto.Worker;
import ru.mts.workflowscheduler.controller.dto.WorkerError;
import ru.mts.workflowscheduler.controller.dto.WorkerExtend;
import ru.mts.workflowscheduler.controller.dto.WorkerLocker;
import ru.mts.workflowscheduler.controller.dto.WorkerIdentity;

import java.util.Set;
import java.util.UUID;

@FeignClient(value = "wf-engine-client")
public interface WorkflowEngineClient {

  @PostMapping("/api/v1/wf/start-for-starters")
  void startWorkflow(ReqWorkflowForStarter req);

  @PostMapping("/api/v1/workers/lock")
  Worker lockWorker(@RequestBody WorkerLocker req);

  @GetMapping("/api/v1/workers/{id}")
  Worker getWorker(@PathVariable UUID id);

  @PostMapping("/api/v1/workers/start")
  void startWorker(@RequestBody WorkerIdentity req);

  @PostMapping("/api/v1/workers/error")
  void errorWorker(@RequestBody WorkerError req);

  @PostMapping("/api/v1/workers/extend")
  void extendWorker(@RequestBody WorkerExtend workerExtend);

  @GetMapping("/api/v1/workers/ids")
  Set<UUID> getWorkerIds(@RequestParam(name = "executor-id") UUID executorId);
}
