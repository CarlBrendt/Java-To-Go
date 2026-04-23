package ru.mts.workflowmail.engine;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mts.workflowmail.controller.dto.ReqWorkflowForStarter;
import ru.mts.workflowmail.controller.dto.ResCount;
import ru.mts.workflowmail.controller.dto.ResIdHolder;
import ru.mts.workflowmail.controller.dto.ResReplacedStarter;
import ru.mts.workflowmail.controller.dto.ResStarterShortListValue;
import ru.mts.workflowmail.controller.dto.Starter;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.controller.dto.WorkerError;
import ru.mts.workflowmail.controller.dto.WorkerExtend;
import ru.mts.workflowmail.controller.dto.WorkerLocker;
import ru.mts.workflowmail.controller.dto.WorkerIdentity;

import java.util.List;
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

  @DeleteMapping("/api/v1/workers/{id}")
  void deleteWorker(@PathVariable UUID id);

  @PutMapping("/api/v1/workers/{id}")
  void replaceWorker(@PathVariable UUID id, @RequestBody String value);

  @PutMapping("/api/v1/starters")
  ResReplacedStarter createOrReplaceStarter(@RequestBody Starter starter);

  @PostMapping("/api/v1/starters/search")
  List<ResStarterShortListValue> findStarters(@RequestBody JsonNode starterSearchingText);

  @PostMapping("/api/v1/starters")
  ResIdHolder createStarter(@RequestBody Starter starter);

  @PutMapping("/api/v1/starters/{id}")
  void replaceStarter(@PathVariable UUID id, @RequestBody JsonNode starter);

  @DeleteMapping("/api/v1/starters/{id}")
  void deleteStarter(@PathVariable UUID id);

  @DeleteMapping("/api/v1/starters")
  void deleteStarter(@RequestBody JsonNode stopStarter);

  @GetMapping("/api/v1/starters/{id}")
  Starter getStarter(@PathVariable UUID id);

  @PostMapping("/api/v1/starters/search/count")
  ResCount findStartersCount(JsonNode starterSearchingText);
}
