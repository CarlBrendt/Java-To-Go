package ru.mts.ip.workflow.engine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.mts.ip.workflow.engine.controller.dto.ReqDebuggingWorkflowExpression;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonError;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.ip.workflow.engine.controller.dto.ResDebugExecutionReport;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ResResolvePlaceholdersExecutionResult;

@Tag(name = "Debugging")
@ApiResponses(value = {
  @ApiResponse(
    responseCode = "405", 
    description = "Method Not Allowed", 
    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonError.class))}
  ),
  @ApiResponse(
    responseCode = "401", 
    description = "Unauthorized", 
    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonError.class))}
  ),
  @ApiResponse(
    responseCode = "500", 
    description = "Internal server error", 
    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonError.class))}
  )
})
@SecurityRequirement(name = "mts-isso")
public interface WorkflowDebuggingApi {

  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200", 
      description = "Definition deployed", 
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResDebugExecutionReport.class))
    ),
    @ApiResponse(
      responseCode = "400", 
      description = "Bad request", 
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/wf/expression/debug", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqDebuggingWorkflowExpression.class)))
  ResponseEntity<String> debug(@RequestBody String expression);

  @ApiResponses(value = {
      @ApiResponse(
          responseCode= "200",
          description = "Definition deployed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResDebugExecutionReport.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v2/wf/expression/debug", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqDebuggingWorkflowExpression.class)))
  ResponseEntity<String> debugV2(@RequestBody String expression);
  
  @ApiResponses(value = {
      @ApiResponse(
          responseCode= "200", 
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivityExecutionContext.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/api/v1/wf/definition/emulate-activity-context/{activityId}", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ActivityExecutionContext.class)))
  ResponseEntity<ActivityExecutionContext> emulateActivityContext(@RequestBody String definition, @PathVariable String activityId);

  
  @PostMapping(value = "/api/v1/placeholders/resolve", produces = "application/json")
  ResponseEntity<String> resolve(@RequestBody ReqResolvePlaceholdersExecutionContext request);
  
  
}
