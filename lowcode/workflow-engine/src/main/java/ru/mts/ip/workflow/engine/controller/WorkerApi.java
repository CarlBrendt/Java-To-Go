package ru.mts.ip.workflow.engine.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqWorkerReplace;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerError;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerExtend;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerLocker;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerIdentity;

import java.util.Set;
import java.util.UUID;

@Tag(name = "Workers")
@SecurityRequirement(name = "mts-isso")
public interface WorkerApi {

  @PostMapping("/api/v1/workers/lock")
  ResponseEntity<ResWorker> getWorkerAndLock(@RequestBody WorkerLocker locker);

  @PostMapping("/api/v1/workers/start")
  void startWorker(@RequestBody WorkerIdentity start);

  @PostMapping("/api/v1/workers/error")
  void errorWorker(@RequestBody WorkerError error);

  @PostMapping("/api/v1/workers/extend")
  void extendWorker(@RequestBody WorkerExtend extend);

  @DeleteMapping("/api/v1/workers/{id}")
  void deleteWorker(@PathVariable UUID id);

  @GetMapping("/api/v1/workers/{id}")
  ResponseEntity<ResWorker> getWorker(@PathVariable UUID id);

  @PutMapping("/api/v1/workers/{id}")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqWorkerReplace.class)))
  void replaceWorker(@PathVariable UUID id, @RequestBody String value);

  @GetMapping("/api/v1/workers/ids")
  Set<UUID> getWorkerIds(@RequestParam(name = "executor-id") UUID executorId);
}
