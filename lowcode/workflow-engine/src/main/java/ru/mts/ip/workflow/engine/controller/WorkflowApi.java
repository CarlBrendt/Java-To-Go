package ru.mts.ip.workflow.engine.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateExecutableWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqDecommission;
import ru.mts.ip.workflow.engine.controller.dto.ReqDefinitionSearching;
import ru.mts.ip.workflow.engine.controller.dto.ReqDefinitionSearchingWithPagination;
import ru.mts.ip.workflow.engine.controller.dto.ReqMessage;
import ru.mts.ip.workflow.engine.controller.dto.ReqRef;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ReqStopWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowAccessList;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowIstanceIdentity;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonError;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.ip.workflow.engine.controller.dto.ResCount;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionListValue;
import ru.mts.ip.workflow.engine.controller.dto.ResExecutableWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ResInstanceHistory;
import ru.mts.ip.workflow.engine.controller.dto.ResRef;
import ru.mts.ip.workflow.engine.controller.dto.ResStopWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowAccessList;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinition.ResRefWithDeprecated;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowInstanceSearchResult;
import ru.mts.ip.workflow.engine.lang.plant.Plant;
import ru.mts.ip.workflow.engine.service.WorkflowExecutionResult;


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
public interface WorkflowApi {

  @Tag(name = "Definition")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Definition deployed",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @PostMapping(value = "/api/v1/wf/definition", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqCreateExecutableWorkflowDefinition.class)))
  ResRefWithDeprecated deployDefinition(@RequestBody String definition, @RequestParam(defaultValue = "true") Boolean ignoreWarnings
      , @RequestParam(defaultValue = "false") Boolean removeDraftOnSuccess);

  @Tag(name = "Definition")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode= "200",
          description = "Definition deployed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @PostMapping(value = "/api/v1/wf/definition/inspect", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqCreateExecutableWorkflowDefinition.class)))
  ResDefinitionInspection inspectDefinition(@RequestBody String definition, @RequestParam(defaultValue = "true") Boolean ignoreWarnings
      , @RequestParam(defaultValue = "false") Boolean removeDraftOnSuccess);

  @Tag(name = "Definition")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Definition ed",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @PostMapping(value = "/api/v1/wf/definition/plant-uml",  produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.TEXT_PLAIN_VALUE)
  ResRef deployDefinitionByPlant(@RequestBody @Schema(example = Plant.EXAMPLE) String plant, @RequestParam(defaultValue = "true") Boolean ignoreWarnings);

  @Tag(name = "Definition")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Ok",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResExecutableWorkflow.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Definition is not found",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @GetMapping("/api/v1/wf/definition/{id}")
  ResExecutableWorkflow getDeployedDefinitionById(@PathVariable UUID id);

 
  @Hidden
  @PostMapping("/api/v1/wf/find-definition-by-ref")
  ResExecutableWorkflow getDeployedDefinitionByRef(@RequestBody ReqRef ref);


  @Tag(name = "Definition")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode= "200",
          description = "Ok",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResExecutableWorkflow.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Definition is not found",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
      )
  })
  @PostMapping("/api/v1/wf/definition/decommission")
  ResExecutableWorkflow decommissionDefinition(@RequestBody ReqDecommission decommission);

  @Tag(name = "Definition")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          content = {@Content(schema = @Schema(example = Plant.EXAMPLE))}
      ),
    @ApiResponse(
      responseCode = "404",
      description = "Definition is not found",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @GetMapping(value = "/api/v1/wf/definition/plant-uml/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
  String getDeployedDefinitionByIdPlant(@PathVariable UUID id);


  @Tag(name = "Definition")
  @PostMapping("/api/v1/wf/definition/search")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqDefinitionSearchingWithPagination.class)))
  List<ResDefinitionListValue> search(@RequestBody(required = false) String req);

  @Tag(name = "Definition")
  @PostMapping("/api/v1/wf/definition/search/count")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqDefinitionSearching.class)))
  ResCount searchCount(@RequestBody(required = false) String req);
 

  @Tag(name = "Draft")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "201",
      description = "Draft created",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Draft already exists",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/api/v1/wf/draft", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqCreateWorkflowDefinition.class)))
  ResRef createDraft(@RequestBody String definition);

  @Tag(name = "Draft")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Draft replaced",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    ),
    @ApiResponse(
      responseCode = "409",
      description = "Draft already exists",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Draft is not found",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @PutMapping(value = "/api/v1/wf/draft/{id}", produces = "application/json")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqCreateWorkflowDefinition.class)))
  ResRef replaceDraft(@RequestBody String definition, @PathVariable UUID id);

  @Tag(name = "Draft")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Ok",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResWorkflowDefinition.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Definition is not found",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @GetMapping("/api/v1/wf/draft/{id}")
  ResWorkflowDefinition getDefinitionDraftById(@PathVariable UUID id);


  @Tag(name = "Draft")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Definition deployed",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @PostMapping(value = "/api/v1/wf/draft-to-definition/{id}", produces = "application/json")
  ResRef draftToDefinition(@PathVariable UUID id, @RequestParam(defaultValue = "true") Boolean ignoreWarnings
      , @RequestParam(defaultValue = "false") Boolean removerDraftOnSuccess);

  @Tag(name = "Draft")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode= "200",
      description = "Definition deployed",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResRef.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
    )
  })
  @PostMapping(value = "/api/v1/wf/definition-to-draft/{id}", produces = "application/json")
  ResRef definitionToDraft(@PathVariable UUID id);

  @Tag(name = "Instance")
  @ApiResponses(value = {
   @ApiResponse(
     responseCode = "200",
       content = {@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(example = Plant.EXAMPLE))}
   ),
   @ApiResponse(
     responseCode = "404",
     description = "Instatnce is not found",
     content = {@Content(mediaType = "application/json",
     schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))}
   )
  })
  @GetMapping(value = "/api/v1/wf/instance/plant-uml/{businessKey}")
  String findWorkflowInstancePlant(@PathVariable @Schema(description = "Business key") String businessKey);

  @Tag(name = "Instance")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    }),
    @ApiResponse(
      responseCode = "404",
      description = "Definition is not found",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    })
  })
  @PostMapping("/api/v1/wf/start")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStartWorkflow.class)))
  ResAsyncStartingResult startWorkflowInstanceAsync(@RequestBody String req, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers);

  @Tag(name = "Instance")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json",
              schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          }),
      @ApiResponse(
          responseCode = "404",
          description = "Definition is not found",
          content = {@Content(mediaType = "application/json",
              schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          })
  })
  @GetMapping("/api/v1/wf/start")
  ResAsyncStartingResult startWorkflowInstanceAsyncGet(@RequestParam Map<String, String> params, @RequestHeader HttpHeaders headers);

  @SecurityRequirement(name = "mts-isso")
  @Tag(name = "Instance")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json",
              schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          }),
      @ApiResponse(
          responseCode = "404",
          description = "Definition is not found",
          content = {@Content(mediaType = "application/json",
              schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          }),
      @ApiResponse(
          responseCode = "200",
          description = "Success",
          content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ResStopWorkflow.class))}
      )
  })
  @PostMapping("/api/v1/wf/stop")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStopWorkflow.class)))
  ResStopWorkflow stopWorkflowInstanceAsync(@RequestBody String req,
                                            @RequestParam(defaultValue = "true") boolean ignoreWarnings,
                                            @RequestParam(defaultValue = "false") boolean showDetails);

  @Hidden
  @Tag(name = "Instance")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    }),
    @ApiResponse(
      responseCode = "404",
      description = "Definition is not found",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    })
  })
  @PostMapping("/api/v1/wf/start-for-sap-inbound")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStartWorkflow.class)))
  ResAsyncStartingResult startWorkflowfromSapInbound(@RequestBody String req, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers);

  @Hidden
  @PostMapping("/api/v1/wf/start-for-starters")
  WorkflowExecutionResult startWorkflowFromStarters(@RequestBody String req, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers);


  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    }),
    @ApiResponse(
      responseCode = "200",
      description = "Ok",
      content = {@Content(mediaType = "text/plain",
      schema = @Schema(implementation = String.class))
    })
  })
  @PostMapping("/api/v1/wf/run")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStartWorkflow.class)))
  ResponseEntity<String> runWorkflowInstance(@RequestBody String req, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers) throws InterruptedException, ExecutionException;

  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json",
          schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          }),
      @ApiResponse(
          responseCode = "200",
          description = "Ok",
          content = {@Content(mediaType = "text/plain",
          schema = @Schema(implementation = String.class))
          })
  })
  @PostMapping(value = "/api/v2/wf/run")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStartWorkflow.class)))
  ResponseEntity<String> runWorkflowInstanceV2(@RequestPart("body") String body, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers, MultipartHttpServletRequest request) throws InterruptedException, ExecutionException;

  @PostMapping(value = "/api/v2/wf/start")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqStartWorkflow.class)))
  ResAsyncStartingResult startWorkflowInstanceAsyncV2(@RequestPart("body") String body, @RequestParam(defaultValue = "true") Boolean ignoreWarnings, @RequestHeader HttpHeaders headers, MultipartHttpServletRequest request) throws InterruptedException, ExecutionException;

  @Tag(name = "Instance")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json",
              schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          }),
      @ApiResponse(
          responseCode = "200",
          description = "Ok",
          content = {@Content(mediaType = "text/plain",
              schema = @Schema(implementation = String.class))
          })
  })
  @GetMapping("/api/v1/wf/run")
  ResponseEntity<String> runWorkflowInstanceCommonParamGet(@RequestParam Map<String, String> params, @RequestHeader HttpHeaders headers) throws InterruptedException, ExecutionException;


  @Tag(name = "Instance")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    })
  })
  @PostMapping("/api/v1/wf/instance/message")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqMessage.class)))
  void signalToWorkflowInstance(@RequestBody String inst, @RequestParam(defaultValue = "true") Boolean ignoreWarnings);

  @Tag(name = "Integrations")
  @SecurityRequirement(name = "mts-isso")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "400",
          description = "Bad request",
          content = {@Content(mediaType = "application/json",
          schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
          })
  })
  @PostMapping("/api/v1/wf/definition/access-list")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqWorkflowAccessList.class)))
  ResRef replaceAccesslist(@RequestBody String accessCommand);

  @GetMapping("/api/v1/wf/definition/{definitionId}/access-list")
  ResWorkflowAccessList getAccessList(@PathVariable UUID definitionId);
  
  @Tag(name = "Instance")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400", 
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    }),
    @ApiResponse(
      responseCode= "200",   
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResWorkflowInstanceSearchResult.class))
    )
  }) 
  @PostMapping("/api/v1/wf/instance/search")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqWorkflowInstanceSearch.class)))
  ResWorkflowInstanceSearchResult searchInstances(@RequestBody(required = false) String search, @RequestParam(defaultValue = "true") boolean manualFilterStatuses);

  @Tag(name = "Instance")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "400", 
      description = "Bad request",
      content = {@Content(mediaType = "application/json",
      schema = @Schema(implementation = ResCommonErrorWithDescriptions.class))
    }),
    @ApiResponse(
      responseCode= "200",   
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResInstanceHistory.class))
    )
  }) 
  @PostMapping("/api/v1/wf/instance/history")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ReqWorkflowIstanceIdentity.class)))
  ResInstanceHistory getInstanceHistory(@RequestBody(required = false) String instanceIdentity);

}
