package ru.mts.ip.workflow.engine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowExpressionForValidate;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowExpressionValidationResult;

@Tag(name = "Validation")
public interface WorkflowValidationApi {

  @SecurityRequirement(name = "mts-isso")
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/wf/expression/validate", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqWorkflowExpressionForValidate.class)))
  ResWorkflowExpressionValidationResult validate(@RequestBody(required = false) String expression);

  @SecurityRequirement(name = "mts-isso")
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/wf/definition/validate", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqCreateExecutableWorkflowDefinition.class)))
  ResWorkflowExpressionValidationResult validateDefinition(@RequestBody(required = false) String expression);

}
