package ru.mts.ip.workflow.engine.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.failure.TimeoutFailure;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.controller.dto.DtoCredentialFilter;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ReqCreateWorkflowDefinitionSchema;
import ru.mts.ip.workflow.engine.controller.dto.ReqDecommission;
import ru.mts.ip.workflow.engine.controller.dto.ReqDefinitionSearchingWithPagination;
import ru.mts.ip.workflow.engine.controller.dto.ReqMessage;
import ru.mts.ip.workflow.engine.controller.dto.ReqRef;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflowSyncSchema;
import ru.mts.ip.workflow.engine.controller.dto.ReqStopWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowAccessList;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.controller.dto.ReqWorkflowIstanceIdentity;
import ru.mts.ip.workflow.engine.controller.dto.ResCount;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionListValue;
import ru.mts.ip.workflow.engine.controller.dto.ResExecutableWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ResInstanceHistory;
import ru.mts.ip.workflow.engine.controller.dto.ResRef;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.ResStopWorkflow;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowAccessList;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowAccessList.ResAccessEntry;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinition;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinition.ResRefWithDeprecated;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowInstanceSearchResult;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqWorkflowForStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.WorkerError;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobSaveOptions;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.dto.WorkflowAccessList;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.lang.DSLCompiler;
import ru.mts.ip.workflow.engine.lang.plant.PlantTail;
import ru.mts.ip.workflow.engine.lang.plant.PlantUtils;
import ru.mts.ip.workflow.engine.service.DefinitionListValue;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowExecutionResult;
import ru.mts.ip.workflow.engine.service.WorkflowInstance;
import ru.mts.ip.workflow.engine.service.WorkflowIstanceIdentity;
import ru.mts.ip.workflow.engine.service.WorkflowService;
import ru.mts.ip.workflow.engine.service.access.AccessService;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobStorage;
import ru.mts.ip.workflow.engine.service.starter.StarterService;
import ru.mts.ip.workflow.engine.service.starter.WorkerService;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.utility.Utils;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationResult;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.DefinitionSearchingSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.DraftWorkflowDefinitionSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.FlowEditorConfigSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.PlantTailSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.ReqWorkflowIstanceIdentitySchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.ReqWorkflowIstanceSearchSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.SignalMessageSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.StartWorkflowFromStarterSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.StartWorkflowSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.StopWorkflowSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.WorkflowAccessListSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.WorkflowDefinitionSchemaFromDraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ru.mts.ip.workflow.engine.validation.schema.v1.WorkflowDefinitionSchemaFromDraft.FLOW_EDITOR_CONFIG;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WorkflowApiController implements WorkflowApi {

  private final ObjectMapper om;
  private final WorkflowService wf;
  private final DtoMapper mapper;
  private final ValidationService validationService;
  private final StarterService starterService;
  private final WorkerService workerService;
  private final JsonSerializer serializer;
  private final AccessService accessService;
  private final @Qualifier("plant") DSLCompiler plantCompiler;
  private final EngineConfigurationProperties properties;
  private final WorkflowExecutorService executor;
  private final BlobStorage blobStorage;
  private final DtoCredentialFilter dtoCredentialFilter;
  private final List<String> securedHeaders = List.of("authorization");

  @Override
  public ResRef createDraft(String definition) {
    var req = validationService.valid(definition, new DraftWorkflowDefinitionSchema(Constraint.NOT_NULL), ReqCreateWorkflowDefinition.class);
    WorkflowDefinition saved = wf.createDraftDefinition(mapper.toWorkflowDefinition(req));
    return new ResRef().setId(saved.getId())
        .setName(saved.getName())
        .setTenantId(saved.getTenantId())
        .setVersion(saved.getVersion());
  }

  @Override
  public ResRef replaceDraft(String definition, UUID id) {
    var req = validationService.valid(definition, new DraftWorkflowDefinitionSchema(Constraint.NOT_NULL), ReqCreateWorkflowDefinition.class);
    WorkflowDefinition saved = wf.replaceDraftDefinition(id, mapper.toWorkflowDefinition(req));
    return new ResRef().setId(saved.getId())
        .setName(saved.getName())
        .setTenantId(saved.getTenantId())
        .setVersion(saved.getVersion());
  }
  
  private ValidationResult validateRuntime(ReqCreateWorkflowDefinition req) {
    WorkflowDefinition toDeploy = mapper.toWorkflowDefinition(req);
    DetailedWorkflowDefinition executable = serializer.toDetailedWorkflowDefinition(toDeploy);
    return validationService.validateRuntime(executable);
  }

  @Transactional
  public ResRefWithDeprecated deployDefinition(ReqCreateWorkflowDefinition req, Boolean ignoreWarnings, Boolean removeDraftOnSuccess) {
    WorkflowDefinition toDeploy = mapper.toWorkflowDefinition(req);
    DetailedWorkflowDefinition executable = serializer.toDetailedWorkflowDefinition(toDeploy);
    toDeploy.setDefaults();
    WorkflowDefinition saved = wf.deploy(toDeploy, removeDraftOnSuccess);
    DefinitionDetails details = executable.getDetails();
    Set<UUID> deprecatedWorkflowIds = new HashSet<>();
    if(details != null) {
      var starters = details.getStarters();
      if(starters != null) {
        for(Starter s : starters) {
          s.setWorkflowDefinitionToStartId(toDeploy.getId());
          Optional.of(starterService.createOrReplaceStarter(s))
              .map(ResReplacedStarter::oldStarter)
              .map(ResReplacedStarter.ResShortStarter::workflowId)
              .ifPresent(deprecatedWorkflowIds::add);
        }
      }
      var initialAccessCommandConfig = details.getInitialAppendAccessConfigCommand();
      if(initialAccessCommandConfig != null) {
        accessService.appendAccessConfig(initialAccessCommandConfig);
      }
    }
    

    return ResRefWithDeprecated.builder()
        .id(saved.getId())
        .name(saved.getName())
        .tenantId(saved.getTenantId())
        .version(saved.getVersion())
        .deprecatedIds(deprecatedWorkflowIds)
        .build();
  }

  @Override
  public ResRef deployDefinitionByPlant(String plant, Boolean ignoreWarnings) {
    String tailPlant = PlantUtils.findTailJsonText(plant);
    validationService.valid(tailPlant, new PlantTailSchema(Constraint.NOT_NULL), PlantTail.class, ignoreWarnings);
    WorkflowDefinition compiled = plantCompiler.compileWorkflowDefinition(plant);
    DetailedWorkflowDefinition executable = serializer.toDetailedWorkflowDefinition(compiled);
    ValidationResult res = validationService.validateRuntime(executable);
    if(res.containCriticalErrors() || (!ignoreWarnings && res.containWarningErrors())) {
      throw new ConstraintViolationException(res.getErrors());
    }
    compiled.setId(UUID.randomUUID());
    WorkflowDefinition saved = wf.deploy(compiled, false);
    return new ResRef().setId(saved.getId()).setName(saved.getName())
        .setTenantId(saved.getTenantId()).setVersion(saved.getVersion());
  }
  

  @Override
  public String findWorkflowInstancePlant(String businessKey) {
    WorkflowInstance inst = wf.getInstance(new WorkflowIstanceIdentity(businessKey))
        .orElseThrow(() -> new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKFLOW_INSTANCE_IS_NOT_FOUND_BY_BK
            , new ErrorMessagePouch().setMessageAargs(new Object[] {businessKey})) );
    return plantCompiler.decompileWorkflowInstance(inst);
  }


  @Override
  @Transactional
  public ResWorkflowDefinition getDefinitionDraftById(UUID id) {
    return mapper.toResWorkflowDefinition(wf.findDraftById(id));
  }

  @Override
  public String getDeployedDefinitionByIdPlant(UUID id) {
    return plantCompiler.decompileWorkflowDefinition(wf.findDeployedById(id));
  }

  @Override
  public ResAsyncStartingResult startWorkflowInstanceAsync(String value, Boolean ignoreWarnings, HttpHeaders headers) {
    var executionResult = startWorkflowInstance(value, ignoreWarnings, headers);
    return new ResAsyncStartingResult().setRunId(executionResult.getRunId()).setBusinessKey(executionResult.getBusinessKey());
  }

  @Override
  public ResAsyncStartingResult startWorkflowInstanceAsyncGet(Map<String, String> params, HttpHeaders headers) {
    String ignoreWarnParamName = "ignoreWarnings";
    String dataParamName = "data";
    boolean ignoreWarnings = Boolean.parseBoolean(params.getOrDefault(ignoreWarnParamName,"true"));
    params.remove(ignoreWarnParamName);
    String data = params.remove(dataParamName);
    if (data == null && !params.isEmpty()) {
      Map<String, Object> flatMap = new HashMap<>(params);
      data = JsonUnflattener.unflatten(flatMap);
    } else if (data == null) {
      data = "{}";
    }
    return startWorkflowInstanceAsync(data, ignoreWarnings, headers);
  }

  @Override
  public ResStopWorkflow stopWorkflowInstanceAsync(String value, boolean ignoreWarnings, boolean showDetails) {
    var req = validationService.valid(value, new StopWorkflowSchema(Constraint.NOT_NULL), ReqStopWorkflow.class, ignoreWarnings);
    var workflowRef = mapper.toWorkflowRef(req.getWorkflowRef());
    var stopInstancesRequested = wf.stop(workflowRef, req.getBusinessKey(), req.isTerminate());
    var resDetails = stopInstancesRequested.stream()
        .map(instance -> new ResStopWorkflow.ResStopWorkflowDetail(instance.getRunId(),instance.getBusinessKey()))
        .toList();
    return showDetails ? new ResStopWorkflow(resDetails) : new ResStopWorkflow(resDetails.size());
  }

  private WorkflowExecutionResult startWorkflowInstance(String value, Boolean ignoreWarnings, HttpHeaders headers) {
    var req = validationService.valid(value, new StartWorkflowSchema(Constraint.NOT_NULL), ReqStartWorkflow.class, ignoreWarnings);
    return startWorkflowInstance(req, headers);
  }
  
  private WorkflowExecutionResult startWorkflowInstance(ReqStartWorkflow req, HttpHeaders headers) {
      try {
        var workflowRef = mapper.toWorkflowRef(req.getWorkflowRef());
        var workflowStartConfig = mapper.toWorkflowStartConfig(req.getWorkflowStartConfig());
        var def = wf.findDeployedDefinition(workflowRef).orElseThrow();
        MDC.put("workflow-ref-id", def.getId().toString());
        var detailedDefinition = serializer.toDetailedWorkflowDefinition(def);
        var variablesWithExposed = Variables.merge(List.of(findExposed(detailedDefinition, headers), workflowStartConfig.getVariables()));
        workflowStartConfig.setVariables(variablesWithExposed);
        return wf.start(detailedDefinition, workflowStartConfig);
      } catch (WorkflowExecutionAlreadyStarted ex) {
        throw new ClientError(HttpStatus.CONFLICT, Errors2.WORKFLOW_ALREADY_STARTED
            , new ErrorMessagePouch().setMessageAargs(new Object[] {ex.getExecution().getWorkflowId()}));
      }
  }
  
  private Variables findExposed(DetailedWorkflowDefinition detailedDefinition, HttpHeaders headers) {
    var exposed = new Variables();
    Optional.ofNullable(detailedDefinition.getDetails())
      .map(DefinitionDetails::getExposedHttpHeaders)
      .filter(f -> !f.isEmpty())
      .ifPresent(httpExposed -> {
        httpExposed.forEach(h -> {
          if(!securedHeaders.contains(h.toLowerCase())) {
            var value = headers.get(h);
            if(value != null) {
              if(value.size() == 1) {
                exposed.put(h, value.get(0));
              } else {
                exposed.put(h, value);
              }
            }
          }
        });
      });
    return exposed;
  }

  @Override
  public void signalToWorkflowInstance(String value, Boolean ignoreWarnings) {
    var req = validationService.valid(value, new SignalMessageSchema(Constraint.NOT_NULL), ReqMessage.class, ignoreWarnings);
    wf.signal(mapper.toSignalWorkflowInstance(req), new Variables(req.getVariables()));
  }

  @Override
  public ResponseEntity<String> runWorkflowInstance(String value, Boolean ignoreWarnings, HttpHeaders headers) throws InterruptedException, ExecutionException {
    var req = validationService.valid(value, new ReqStartWorkflowSyncSchema(Constraint.NOT_NULL), ReqStartWorkflow.class, ignoreWarnings);
    Duration timeout = Optional
        .ofNullable(req.getWorkflowStartConfig().getExecutionTimeout())
        .map(Duration::parse)
        .orElse(Duration.ofSeconds(properties.getSyncStartTimeoutDefaultSeconds()));
    long seconds = timeout.get(ChronoUnit.SECONDS);
    WorkflowExecutionResult result = startWorkflowInstance(req, headers);
    try {
      Variables variables = result.getResult(seconds, TimeUnit.SECONDS);
      var node = Optional.ofNullable(variables.getPlaneValue())
          .filter(n -> !n.isNull()) 
          .orElseGet(() -> variables.asNode());
      node = blobStorage.resolvePlaceholders(node);
      boolean isTextual = node.isTextual();
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.set("XMTS_IP_LC_RUN_ID", result.getRunId());
      responseHeaders.set("XMTS_IP_LC_BK", result.getBusinessKey());
      return ResponseEntity.ok()
          .headers(responseHeaders)
          .contentType(isTextual ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON)
          .body(isTextual ? node.textValue() : node.toString());
    } catch (TimeoutException ex) {
      throw ClientError.syncRunTimeout();
    } catch (ExecutionException ex){
      Optional.ofNullable(ex.getCause())
          .map(Throwable::getCause)
          .filter(cause -> cause instanceof TimeoutFailure)
          .ifPresent(cause -> {throw ClientError.syncRunTimeout();});
      throw ex;
    }
  }

  @Override
  public ResponseEntity<String> runWorkflowInstanceCommonParamGet(Map<String, String> params, HttpHeaders headers)
      throws InterruptedException, ExecutionException {

    String ignoreWarnParamName = "ignoreWarnings";
    String dataParamName = "data";
    boolean ignoreWarnings = Boolean.parseBoolean(params.getOrDefault(ignoreWarnParamName,"true"));
    params.remove(ignoreWarnParamName);
    String data = params.remove(dataParamName);
    if (data == null && !params.isEmpty()) {
      Map<String, Object> flatMap = new HashMap<>(params);
      data = JsonUnflattener.unflatten(flatMap);
    } else if (data == null) {
      data = "{}";
    }
    return runWorkflowInstance(data, ignoreWarnings, headers);
  }

  @Override
  @Transactional
  public ResExecutableWorkflow getDeployedDefinitionById(UUID id) {
    var executable = serializer.toDetailedWorkflowDefinition(wf.findDeployedById(id));
    return mapper.toResExecutableWorkflow(dtoCredentialFilter.filter(executable));
  }

  @Override
  @Transactional
  public ResExecutableWorkflow decommissionDefinition(ReqDecommission decommission) {
    return mapper.toResExecutableWorkflow(dtoCredentialFilter.filter(wf.decommissionById(decommission.id())));
  }


  @Override
  public List<ResDefinitionListValue> search(String req) {
    ReqDefinitionSearchingWithPagination searching = new ReqDefinitionSearchingWithPagination();
    if(req != null) {
      searching = validationService.valid(req, new DefinitionSearchingSchema(Constraint.NOT_NULL), ReqDefinitionSearchingWithPagination.class);
    } else {
      searching = new ReqDefinitionSearchingWithPagination();
    }
    List<DefinitionListValue> resultList = wf.searchDefinitions(mapper.toDefinitionSearching(searching));
    return mapper.toResDefinitionListValues(resultList);
  }
  

  @Override
  public ResCount searchCount(String req) {
    ReqDefinitionSearchingWithPagination searching = new ReqDefinitionSearchingWithPagination();
    if(req != null) {
      searching = validationService.valid(req, new DefinitionSearchingSchema(Constraint.NOT_NULL), ReqDefinitionSearchingWithPagination.class);
    } else {
      searching = new ReqDefinitionSearchingWithPagination();
    }
    return new ResCount(wf.searchDefinitionsCount(mapper.toDefinitionSearching(searching)));
  }
  
  private ReqCreateWorkflowDefinition validateDefinition(String req, Boolean ignoreWarnings, BaseSchema validationSchema) {
    var result = validationService.validateAndParse(req, validationSchema, ReqCreateWorkflowDefinition.class);
    ReqCreateWorkflowDefinition def = result.getParseResult();
    var validationResult = result.getValidationResult();
    if (validationResult.containCriticalErrors()) {
      throw new ConstraintViolationException(validationResult.getErrors());
    } else {
      def.setCompiled(executor.applyExpressionDefualts(def.getCompiled()));
      var runtimeValidationResult = validateRuntime(def);
      validationResult.getErrors().addAll(runtimeValidationResult.getErrors());
    }
    if(validationResult.containCriticalErrors()) {
      throw new ConstraintViolationException(validationResult.getErrors());
    } else {
      if(!ignoreWarnings) {
        if(validationResult.containWarningErrors()) {
          throw new ConstraintViolationException(validationResult.getErrors());
        }
      }
      return def;
    }
  }
  
  @Override
  @Transactional
  public ResRefWithDeprecated deployDefinition(String req, Boolean ignoreWarnings, Boolean removeDraftOnSuccess) {
    var def = validateDefinition(req, ignoreWarnings, new ReqCreateWorkflowDefinitionSchema(Constraint.NOT_NULL));
    var res =  deployDefinition(def, ignoreWarnings, removeDraftOnSuccess);
    res.setStartUrl(getStartUrl());
    return res;
  }

  @Override
  public ResDefinitionInspection inspectDefinition(String req, Boolean ignoreWarnings,
      Boolean removeDraftOnSuccess) {
    var schema = new ReqCreateWorkflowDefinitionSchema(Map.of(FLOW_EDITOR_CONFIG, new FlowEditorConfigSchema()), Constraint.NOT_NULL);

    var def = validateDefinition(req, ignoreWarnings, schema);
    return wf.inspectDefinition(mapper.toWorkflowDefinition(def));
  }

  private String getStartUrl() {
    return Optional.ofNullable(properties.getStartUrl()).filter(v -> !v.isBlank()).orElseGet(
        () -> getCurrentHttpRequest().map(r -> r.getRequestURL().toString())
        .map(url -> UriComponentsBuilder.fromHttpUrl(url).replacePath("/api/v1/wf/start").build().toString()).orElse(null)
    );
  }

  @Override
  @Transactional
  public ResRef draftToDefinition(UUID id, Boolean ignoreWarnings, Boolean removeDraftOnSuccess) {
    try {
      var draft = wf.findDraftById(id).setId(null);
      var def = validateDefinition(om.writeValueAsString(draft), ignoreWarnings, new WorkflowDefinitionSchemaFromDraft(Constraint.NOT_NULL));
      return deployDefinition(def, ignoreWarnings, removeDraftOnSuccess);
    } catch (JsonProcessingException ex) {
      log.error("Deploy definition failed", ex);
      throw new IllegalStateException("Deploy definition failed");
    }
  }
  

  @Override
  public ResRef definitionToDraft(UUID id) {
    try {
      return createDraft(om.writeValueAsString(wf.findDeployedById(id)));
    } catch (JsonProcessingException ex) {
      log.error("Draft definition failed", ex);
      throw new IllegalStateException("Draft definition failed");
    }
  }

  @Override
  public ResAsyncStartingResult startWorkflowfromSapInbound(String req, Boolean ignoreWarnings, HttpHeaders headers) {
    var executionResult = startWorkflowInstance(req, ignoreWarnings, headers);
    return new ResAsyncStartingResult().setRunId(executionResult.getRunId()).setBusinessKey(executionResult.getBusinessKey());
  }

  @Override
  public WorkflowExecutionResult startWorkflowFromStarters(String req, Boolean ignoreWarnings,
      HttpHeaders headers) {
    var reqWorkflow = validationService.valid(req, new StartWorkflowFromStarterSchema(Constraint.NOT_NULL), ReqWorkflowForStarter.class, ignoreWarnings);
    var workerIdentity = reqWorkflow.getWorkerIdentity();
    workerService.checkWorkerExists(workerIdentity);
    try {
      var result = startWorkflowInstance(reqWorkflow.getStartWorkflow(), headers);
      workerService.successWorker(workerIdentity.workerId());
      return result;
    } catch (Exception ex) {
      workerService.errorWorker(WorkerError.fromThrowable(workerIdentity.workerId(),workerIdentity.executorId(), ex));
      throw ex;
    }
  }

  @Override
  public ResRef replaceAccesslist(String accessConfiguration) {
    ReqWorkflowAccessList req = validationService.valid(accessConfiguration, new WorkflowAccessListSchema(Constraint.NOT_NULL), ReqWorkflowAccessList.class, true);
    WorkflowAccessList accessConfig = mapper.toReqWorkflowAccessConfiguration(req);
    accessService.replaceAccessList(accessConfig);
    return new ResRef().setStartUrl(getStartUrl());
  }

  @Override
  public ResWorkflowAccessList getAccessList(UUID definitionId) {
    var accessEntries =
        accessService.getClientIds(definitionId).stream().map(ResAccessEntry::new).toList();
    return new ResWorkflowAccessList(accessEntries);
  }

  private static Optional<HttpServletRequest> getCurrentHttpRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest);
  }

  @Override
  public ResWorkflowInstanceSearchResult searchInstances(String req, boolean manualFilterStatuses) {
    var searchConfig = parseValidWorkflowIstanceSearch(req);
    var res =  mapper.toResWorkflowInstanceSearchResult(wf.searchInstances(parseValidWorkflowIstanceSearch(req)));
    if(manualFilterStatuses && searchConfig.getExecutionStatus() != null) {
      res.setValues(res.getValues()
          .stream()
          .filter(inst -> Objects.equals(inst.getStatus(), searchConfig.getExecutionStatus()))
          .toList());
    }
    return res;
  }

  private WorkflowInstanceSearch parseValidWorkflowIstanceSearch(String req) {
    return Optional.ofNullable(req)
        .map(json -> validationService.valid(req, new ReqWorkflowIstanceSearchSchema(Constraint.NOT_NULL), ReqWorkflowInstanceSearch.class))
        .map(mapper::toWorkflowInstanceSearch)
        .orElseGet(WorkflowInstanceSearch::new);
  }

  @Override
  public ResInstanceHistory getInstanceHistory(String instanceIdentityText) {
    var req = validationService.valid(instanceIdentityText, new ReqWorkflowIstanceIdentitySchema(Constraint.NOT_NULL), ReqWorkflowIstanceIdentity.class);
    WorkflowIstanceIdentity identity = mapper.toWorkflowIstanceIdentity(req);
    return mapper.toResInstanceHistory(wf.getInstanceHistory(identity));
  }

  @Override
  public ResExecutableWorkflow getDeployedDefinitionByRef(ReqRef reqRef) {
    Ref ref = mapper.toWorkflowRef(reqRef);
    return wf.findDeployedDefinition(ref)
      .map(mapper::toExecutableWorkflowDefinition)
      .map(dtoCredentialFilter::filter)
      .map(mapper::toResExecutableWorkflow)
      .orElse(null);
  }

  private Map<String, JsonNode> compileFileVariables(MultipartHttpServletRequest request, BlobSaveOptions blobSaveOptions) {
    Map<String, JsonNode> result = new HashMap<>();

    var fm = request.getFileMap();
    List<Entry<String, MultipartFile>> validFiles = fm.entrySet()
        .stream()
        .filter(es -> !es.getValue().isEmpty())
        .collect(Collectors.toList());

    try {
      for (var file : validFiles) {
        String varName = file.getKey();
        String fileName = file.getValue().getOriginalFilename();
        String fileExtension = Utils.getFileExtension(fileName);
        BlobRef saved;
        if (Const.FileExtension.TEXT_EXTENSIONS.contains(fileExtension)) {
          String content = new String(file.getValue().getBytes(), StandardCharsets.UTF_8);
          JsonNode node = toJsonNode(content);
          saved = blobStorage.save(node, blobSaveOptions);
        } else {
          saved = blobStorage.saveFile(file.getValue().getResource(), blobSaveOptions);
        }
        result.put(varName, new TextNode(saved.asLowCodeDecorateVariableRef()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  private JsonNode toJsonNode(String content) throws JsonProcessingException {
    JsonNode node;
    try {
      node = om.readTree(content);
    } catch (JsonParseException e){
      node = TextNode.valueOf(content);
    }
    return node;
  }

  @Override
  public ResponseEntity<String> runWorkflowInstanceV2(String body, Boolean ignoreWarnings,
      HttpHeaders headers, MultipartHttpServletRequest request)
      throws InterruptedException, ExecutionException {
    return runWorkflowInstance(prepareFileVariables(body, ignoreWarnings, headers, request), ignoreWarnings, headers);
  }

  private String prepareFileVariables(String body, Boolean ignoreWarnings,
      HttpHeaders headers, MultipartHttpServletRequest request) {
    var req = validationService.valid(body, new ReqStartWorkflowSyncSchema(Constraint.NOT_NULL), ReqStartWorkflow.class, ignoreWarnings);

    var expirationDate = getBlobSaveOptions(req, properties);
    var fileVars = compileFileVariables(request, expirationDate);
    var config = req.getWorkflowStartConfig();
    if(config != null) {
      var vars = config.getVariables();
      if(vars != null) {
        vars.putAll(fileVars);
      } else {
        config.setVariables(fileVars);
      }
    }
    return serializer.toJson(req).toString();
  }

  private BlobSaveOptions getBlobSaveOptions(ReqStartWorkflow reqStartWorkflow, EngineConfigurationProperties engineConfig) {
    Duration workflowMaxDuration = null;
    var executionConfig = reqStartWorkflow.getWorkflowStartConfig();
    var definitionId = Optional.ofNullable(reqStartWorkflow.getWorkflowRef()).map(ReqRef::getId).orElse(null);
    try {
      workflowMaxDuration = Duration.parse(executionConfig.getExecutionTimeout());
    } catch (DateTimeParseException ignore){
    }

    if (workflowMaxDuration == null){
      workflowMaxDuration = Duration.ofSeconds(engineConfig.getWorkflowExecutionConfig().getDefaultExecutionTimeoutSeconds());
    }

    workflowMaxDuration = workflowMaxDuration.plusDays(15);
    OffsetDateTime expirationDate = DateHelper.today().plus(workflowMaxDuration);
    return new BlobSaveOptions()
        .setExpirationDate(expirationDate)
        .setBusinessKey(executionConfig.getBusinessKey())
        .setWorkflowDefinitionId(definitionId);
  }

  @Override
  public ResAsyncStartingResult startWorkflowInstanceAsyncV2(String body, Boolean ignoreWarnings,
      HttpHeaders headers, MultipartHttpServletRequest request)
      throws InterruptedException, ExecutionException {
    return startWorkflowInstanceAsync(prepareFileVariables(body, ignoreWarnings, headers, request), ignoreWarnings, headers);
  }

}
