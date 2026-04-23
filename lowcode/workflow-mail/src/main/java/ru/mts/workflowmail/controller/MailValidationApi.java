package ru.mts.workflowmail.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.mts.workflowmail.controller.dto.ReqMailStarterConfig;
import ru.mts.workflowmail.controller.dto.ResCommonError;
import ru.mts.workflowmail.controller.dto.ResValidationResult;

@Tag(name = "Scheduler validation")
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
public interface MailValidationApi {

  @ResponseStatus(HttpStatus.OK)
  @SecurityRequirement(name = "mts-isso")
  @PostMapping("/api/v1/consumer/validate")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqMailStarterConfig.class)))
  ResValidationResult validateStarterConfig(@RequestBody String starter);


  @ResponseStatus(HttpStatus.OK)
  @SecurityRequirement(name = "mts-isso")
  @PostMapping("/api/v1/consumer/workflow-compatibility/validate")
  ResValidationResult validateStarterCompatibility(@RequestBody String req);
}
