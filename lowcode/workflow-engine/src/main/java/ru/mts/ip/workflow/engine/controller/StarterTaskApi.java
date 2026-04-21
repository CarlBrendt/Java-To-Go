package ru.mts.ip.workflow.engine.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.mts.ip.workflow.engine.controller.dto.ReqStarterTask;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterTask;

import java.util.UUID;

@Tag(name = "Starter tasks")
@SecurityRequirement(name = "mts-isso")
public interface StarterTaskApi {

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/api/v1/starters/tasks/{id}")
  void stopSapTask(@PathVariable UUID id);

  @PostMapping("/api/v1/starters/tasks")
  ResIdHolder save(@RequestBody ReqStarterTask task);

  @PostMapping("/api/v1/starters/tasks/{id}")
  ResStarterTask restartSapTask(@PathVariable UUID id);

  @GetMapping("/api/v1/starters/tasks/{id}")
  ResStarterTask getSapTask(@PathVariable UUID id);
}
