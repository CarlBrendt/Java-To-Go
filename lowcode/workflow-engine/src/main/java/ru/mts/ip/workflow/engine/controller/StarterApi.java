package ru.mts.ip.workflow.engine.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonError;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.ip.workflow.engine.controller.dto.ResCount;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.ResStarterShortListValue;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterV2;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterSearching;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;

import java.util.List;
import java.util.UUID;

@Tag(name = "Starters")
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
@RequestMapping("/api/v1/starters")
public interface StarterApi {

  @ResponseStatus(HttpStatus.OK)
  @PutMapping
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStarterV2.class)))
  ResReplacedStarter createOrReplaceStarter(@RequestBody String starter);

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStarterV2.class)))
  ResIdHolder createStarter(@RequestBody String starter);

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("/{id}")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStarterV2.class)))
  void replaceStarter(@PathVariable UUID id, @RequestBody String starter);

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResStarter getStarter(@PathVariable UUID id);

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @SecurityRequirement(name = "mts-isso")
  @DeleteMapping("/{id}")
  void deleteStarter(@PathVariable UUID id);

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping
  @Validated
  void deleteStarter(@RequestBody @Valid ReqStopStarter reqStopStarter);

  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/search")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStarterSearching.class)))
  List<ResStarterShortListValue> findStarters(@RequestBody(required = false) String starterSearching);

  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping("/search/count")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStarterSearching.class)))
  ResCount findStartersCount(@RequestBody(required = false) String starterSearching);


  @PatchMapping("/{id}")
  void partialUpdateStarter(
      @PathVariable UUID id,
      @RequestBody ReqStarterPatch starterPatch);
}
