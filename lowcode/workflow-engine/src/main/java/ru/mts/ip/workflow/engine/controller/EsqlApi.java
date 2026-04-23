package ru.mts.ip.workflow.engine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.mts.ip.workflow.engine.controller.dto.ResEsqlToLuaTaskState;

@Tag(name = "ESQL to Lua compilation")
@SecurityRequirement(name = "mts-isso")
public interface EsqlApi {

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/esql-to-lua/tasks", produces = "application/json")
  ResEsqlToLuaTaskState createCompilationTask(@RequestBody String task);

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/api/v1/esql-to-lua/tasks/{id}", produces = "application/json")
  ResEsqlToLuaTaskState getCompilationTaskState(@PathVariable String id);

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/esql-to-lua/tasks/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter sse(@RequestBody String task);

}
