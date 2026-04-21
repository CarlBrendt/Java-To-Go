package ru.mts.ip.workflow.engine.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterExclusion;

import java.util.List;
import java.util.UUID;

public interface StarterExclusionApi {

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @SecurityRequirement(name = "mts-isso")
  @DeleteMapping("/api/v1/starters/exclusions/{id}")
  void deleteStarterExclusion(@PathVariable UUID id);

  @ResponseStatus(HttpStatus.CREATED)
  @SecurityRequirement(name = "mts-isso")
  @PostMapping("/api/v1/starters/exclusions")
  ResIdHolder createStarterExclusion(@RequestBody String value);

  @GetMapping("/api/v1/starters/{starterId}/exclusions")
  List<ResStarterExclusion> getStarterExclusions(@PathVariable UUID starterId);
}
