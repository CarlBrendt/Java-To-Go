package ru.mts.ip.workflow.engine.service;

import static ru.mts.ip.workflow.engine.Const.DefinitionAvailabilityStatus.DECOMMISSIONED;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.micrometer.observation.annotation.Observed;
import io.temporal.client.WorkflowNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection.Auth;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection.InspectionApiInfo;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection.InspectionImsInfo;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobSaveOptions;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledActivity;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledRestCallConfig;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledRestCallTemplateDef;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledWorkflowCall;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledWorkflowDef;
import ru.mts.ip.workflow.engine.dto.DefinitionCompiled.CompiledWorkflowDefDetails;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.EventCorrelation;
import ru.mts.ip.workflow.engine.dto.FlowEditorConfig;
import ru.mts.ip.workflow.engine.dto.IntpApi.Product;
import ru.mts.ip.workflow.engine.dto.IntpApi.Stand;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.json.JsonValuesSizeOptimizer;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepository;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;
import ru.mts.ip.workflow.engine.service.access.AccessService;
import ru.mts.ip.workflow.engine.service.starter.StarterService;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchListValue;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchResult;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;
import ru.mts.ip.workflow.engine.validation.VariableValidator;

@Slf4j
@Service
@Observed
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

  private final WorkflowExecutionEngine engine;
  private final WorkflowDefinitionRepository repo;
  private final WorkflowDefinitionRepositoryHelper repoCustom;
  private final VariableValidator variableValidator;
  private final JsonSerializer serializer;
  private final ErrorCompiler errorCompiler;
  private final AccessService accessService;
  private final JsonValuesSizeOptimizer jsonObjectOptimizer;
  private final StarterService starterService;
  private final XsdService xsdService;
  private final EngineConfigurationProperties engineConfig;

  @Override
  @Transactional
  public WorkflowDefinition deploy(WorkflowDefinition definition, boolean removerDraftOnSuccess) {
    definition.setDefaults();
    if (repo.existsById(definition.getId())) {
      throw new ConstraintViolationException(List.of(errorCompiler.error(Errors2.WORKFLOW_ALREADY_EXISTS)));
    } else {
      Optional<WorkflowVersion> wv = repo.findFirstVersionByNameAndTenantIdAndStatusOrderByVersionDesc(definition.getName()
          , definition.getTenantId(), Const.DefinitionStatus.DEPLOYED);

      repo.markAllNotLatest(definition.getName(), definition.getTenantId(), Const.DefinitionStatus.DEPLOYED);

      Integer version = wv.map(WorkflowVersion::getVersion).orElse(0);
      WorkflowDefinition res = repo.save(definition.setVersion(version + 1).setStatus(Const.DefinitionStatus.DEPLOYED).setLatest(true));
      if(removerDraftOnSuccess) {
        repo.markRemoved(definition.getName(), definition.getTenantId(), Const.DefinitionStatus.DRAFT, definition.getVersion());
      }
      return res;
    }
  }

  @Override
  public WorkflowExecutionResult start(DetailedWorkflowDefinition definition, WorkflowStartConfig executionConfig) {
    accessService.findAccessTroubleToStartWorkflow(definition).ifPresent(errors2 -> {
      throw new ClientError(HttpStatus.FORBIDDEN, errors2, new ErrorMessagePouch().setMessageAargs(new Object[] {}));
    });

   var resolvedVars = jsonObjectOptimizer.resolvePlaceholders(executionConfig.getVariables().asNode());

      Optional.ofNullable(definition.getDetails())
      .map(DefinitionDetails::getInputValidateSchema)
      .ifPresent(schema -> {
        var validationResult = variableValidator.validateVariables(schema, new Variables(resolvedVars));
        if(!validationResult.isEmpty()) {
          throw new ClientError(validationResult);
        }
      });
      Optional.ofNullable(definition.getDetails())
      .map(DefinitionDetails::getXsdValidation)
      .ifPresent(xsdValidation -> {
        List<ClientErrorDescription> xsdErrors = xsdService.validateJsonWithXml(resolvedVars, xsdValidation);
        if(!xsdErrors.isEmpty()) {
          throw new ClientError(xsdErrors);
        }
      });

    Optional.ofNullable(definition.getDetails())
        .map(DefinitionDetails::getYamlValidation)
        .ifPresent(yamlValidation -> {
          List<ClientErrorDescription> yamlErrors = variableValidator.validateYamlVariables(resolvedVars, yamlValidation);
          if(!yamlErrors.isEmpty()) {
            throw new ClientError(yamlErrors);
          }
        });

      BlobSaveOptions blobSaveOptions = getBlobSaveOptions(definition, executionConfig, engineConfig);
      optimizeVariables(executionConfig.getVariables(), blobSaveOptions);
      return engine.execute(definition, executionConfig);
  }

  private BlobSaveOptions getBlobSaveOptions(DetailedWorkflowDefinition definition, WorkflowStartConfig executionConfig, EngineConfigurationProperties engineConfig) {
    EngineConfigurationProperties.WorkflowExecutionDefaultConfig config = engineConfig.getWorkflowExecutionConfig();
    Duration workflowMaxDuration = Optional.ofNullable(executionConfig.getExecutionTimeout())
        .orElse(Duration.ofSeconds(config.getDefaultExecutionTimeoutSeconds()));

    workflowMaxDuration = workflowMaxDuration.plusDays(15);
    OffsetDateTime expirationDate = DateHelper.today().plus(workflowMaxDuration);
    return new BlobSaveOptions()
        .setExpirationDate(expirationDate)
        .setBusinessKey(executionConfig.getBusinessKey())
        .setWorkflowDefinitionId(definition.getId());
  }

  @Override
  public void signal(EventCorrelation sig, Variables variables) {
    engine.signal(sig, variables);
  }

  @Override
  public Optional<WorkflowDefinition> findById(UUID id) {
    return repo.findById(id);
  }

  @Override
  public Optional<WorkflowDefinition> findDeployedDefinition(Ref workflowRef) {
    return repoCustom.findDeployedDefinition(workflowRef);
  }

  @Override
  @Transactional
  public WorkflowDefinition createDraftDefinition(WorkflowDefinition definition) {
    definition.setDefaults();
    Optional<WorkflowVersion> lastVersion = repo.findFirstVersionByNameAndTenantIdAndStatusOrderByVersionDesc(definition.getName(), definition.getTenantId(), Const.DefinitionStatus.DRAFT);
    Integer ver = lastVersion.map(WorkflowVersion::getVersion).orElse(0);
    definition.setVersion(ver + 1).setLatest(true);
    lastVersion.ifPresent(ld -> {
      repo.markNotLatest(ld.getId());
    });
    return repo.save(definition);
  }

  @Override
  public WorkflowDefinition replaceDraftDefinition(UUID uuid, WorkflowDefinition definition) {
    definition.setDefaults();
    var oldDefinition = repo.findById(uuid).orElseThrow(() -> new ClientError(HttpStatus.NOT_FOUND
        , Errors2.DRAFT_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {uuid})));
    if(oldDefinition.getName().equals(definition.getName()) 
        && oldDefinition.getTenantId().equals(definition.getTenantId())) {
    } else if (repo.existsByNameAndTenantIdAndStatusAndDeleted(definition.getName(), definition.getTenantId(), Const.DefinitionStatus.DRAFT , false)){
      var pouch = new ErrorMessagePouch().setMessageAargs(new Object[] {definition.getName(), definition.getTenantId()});
      throw new ClientError(HttpStatus.CONFLICT, Errors2.DRAFT_DEFINITION_ALREADY_EXISTS, pouch);
    }
    return repo.save(definition.setId(uuid).setCreateTime(oldDefinition.getCreateTime()).setVersion(oldDefinition.getVersion()));
  }

  @Override
  public Optional<WorkflowInstance> getInstance(WorkflowIstanceIdentity identity) {
    return engine.getInstance(identity);
  }

  @Override
  public Optional<DetailedWorkflowDefinition> findExecutableDefinition(Ref workflowRef) {
    return repoCustom.findDeployedDefinition(workflowRef).map(serializer::toDetailedWorkflowDefinition);
  }

  @Override
  public List<WorkflowInstanceSearchListValue> stop(Ref workflowRef, String businessKey, boolean terminate) {
    WorkflowInstanceSearch search = new WorkflowInstanceSearch();
    if (businessKey != null) {
      search.setBusinessKey(businessKey);
    } else {
      if (workflowRef != null && workflowRef.getId() != null) {
        var def = findDeployedDefinition(workflowRef).orElseThrow();
        workflowRef.setName(def.getName());
        workflowRef.setVersion(def.getVersion());
        workflowRef.setTenantId(def.getTenantId());
      }
      String workflowName = Optional.ofNullable(workflowRef).map(Ref::getName).orElse(null);
      search.setWorkflowName(workflowName);
    }
    search.setExecutionStatus(Const.WorkflowInstanceStatus.RUNNING);

    var instances = engine.searchInstances(search);

    if (businessKey != null && instances.getValues().isEmpty()) {
      throw new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKFLOW_RUNNING_NOT_FOUND_BY_BK, new ErrorMessagePouch().setMessageAargs(new Object[] {businessKey}));
    }
    var version = Optional.ofNullable(workflowRef).map(Ref::getVersion).map(Long::valueOf);
    var tenantId = Optional.ofNullable(workflowRef).map(Ref::getTenantId).orElse(Const.DEFAULT_TENANT_ID);

    List<WorkflowInstanceSearchListValue> resDetails = new ArrayList<>();
    for (WorkflowInstanceSearchListValue instanceValue : instances.getValues()) {
      boolean versionMatched = version.map(v->v.equals(instanceValue.getWorkflowVersion())).orElse(Boolean.TRUE);
      boolean tenantMatched = tenantId.equals(instanceValue.getTenantId());
      if (Objects.nonNull(businessKey) || versionMatched && tenantMatched) {
        String bKey = instanceValue.getBusinessKey();
        boolean isRequested = requestStop(bKey, terminate);
        if (isRequested) {
          resDetails.add(instanceValue);
        }
      }
    }

    return resDetails;
  }

  @Override
  @Transactional
  public DetailedWorkflowDefinition decommissionById(UUID id) {
    var definition = findDeployedById(id);
    if (definition.getAvailabilityStatus().equals(DECOMMISSIONED)) {
      throw new ClientError(HttpStatus.CONFLICT, Errors2.WORKFLOW_CHANGE_AVAILABILITY_CONFLICT,
          new ErrorMessagePouch().setMessageAargs(new Object[] {id, DECOMMISSIONED}));
    }
    definition.setAvailabilityStatus(DECOMMISSIONED);

    starterService.softDeleteStarterByWorkflowDefId(id);
    return serializer.toDetailedWorkflowDefinition(definition);
  }

  @Override
  public ResDefinitionInspection inspectDefinition(WorkflowDefinition def) {
    var compiled = def.getCompiled();
    var details = def.getDetails();
    DefinitionCompiled definitionCompiled =
        serializer.treeToValue(compiled, DefinitionCompiled.class);
    FlowEditorConfig flowEditorConfig =
        serializer.treeToValue(def.getFlowEditorConfig(), FlowEditorConfig.class);
    DefinitionDetails definitionDetails =
        serializer.treeToValue(details, DefinitionDetails.class);
    var activities = Optional.ofNullable(definitionCompiled)
        .map(DefinitionCompiled::activities)
        .orElse(List.of());
    var intoApis = activities.stream()
        .map(this::toInspectionApiInfo)
        .filter(Objects::nonNull)
        .toList();
    var imsList = findImInfo(definitionCompiled, definitionDetails, flowEditorConfig);
    var res = new ResDefinitionInspection(intoApis, imsList);
    return res;
  }
  
  private List<InspectionImsInfo> findImInfo(
      DefinitionCompiled definitionCompiled
      , DefinitionDetails definitionDetails
      , FlowEditorConfig flowEditorConfig) {
    
    List<InspectionImsInfo> result = new ArrayList<>();
    var activityMetadata = flowEditorConfig.activityMetadata();
    var startMetadata = flowEditorConfig.startMetadata();
    
    var starterImsInfo = Optional.ofNullable(definitionDetails.getStarters()).orElse(List.of()).stream().filter(s -> !Const.StarterType.REST_CALL.equals(s.getType()))
      .findFirst().map(filtered -> new InspectionImsInfo().setType(filtered.getType()).setName(filtered.getName()))
      .orElse(null);
    
    if(starterImsInfo != null && startMetadata != null) {
      starterImsInfo.setIms(startMetadata.ims());
      result.add(starterImsInfo);
    }
    
    var activityMap = definitionCompiled.activities().stream().collect(Collectors.toMap(k -> k.id(), v -> v));
    if(activityMetadata != null) {
      activityMetadata.forEach((id, act) -> {
        var compiledActivity = activityMap.get(id);
        if(compiledActivity == null) {
          throw new IllegalStateException("Activity is not found " + id);
        }
        if (compiledActivity.workflowCall() != null) {
          Set<String> ignoredTypes = Set.of(Const.WorkflowType.TRANSFORM, Const.WorkflowType.XSLT_TRANSFORM);
          var type = compiledActivity.workflowCall().workflowDef().type();

          if (!ignoredTypes.contains(type)) {
            var imsactivityInfo = new InspectionImsInfo().setIms(act.ims())
                .setName(id)
                .setType(type);
            result.add(imsactivityInfo);
          }
        }
      });
    }
    return result;
    
  }
  
  private InspectionApiInfo toInspectionApiInfo(CompiledActivity act) {
    InspectionApiInfo res = null;
    if(Objects.equals(act.type(), Const.ActivityType.WORKFLOW_CALL)) {
      var optDetails = Optional.ofNullable(act.workflowCall())
      .map(CompiledWorkflowCall::workflowDef)
      .map(CompiledWorkflowDef::details);
      
      var intpApi = optDetails.map(CompiledWorkflowDefDetails::intpApi).orElse(null);
      var auth = optDetails.map(CompiledWorkflowDefDetails::restCallConfig)
      .map(CompiledRestCallConfig::restCallTemplateDef)
      .map(CompiledRestCallTemplateDef::authDef).orElse(null);
      
      if(intpApi != null && auth != null) {
        res = new InspectionApiInfo();
        res.setIms(intpApi.getIms());
        Optional.ofNullable(intpApi.getVersionId())
          .filter(vid -> vid != null && !vid.isBlank())
          .or(() -> Optional.ofNullable(intpApi.getProduct())
              .map(Product::getVersionId)
           ).ifPresent(res::setVersionId);
        Optional.ofNullable(intpApi.getStand())
          .map(Stand::getId).ifPresent(res::setStand);
        
        var oauth2 = auth.oauth2();
        if(oauth2 != null) {
          var inspectAuth = new Auth().setClientId(oauth2.clientId())
              .setServiceAccount(intpApi.getServiceAccount())
              .setType(intpApi.getSubscriptionAuth())
              .setSecret(oauth2.clientSecret());
          splitValutSecret(inspectAuth);
          res.setAuth(inspectAuth);        
        }
        
      }
    }
    return res;
  }
  
  private void splitValutSecret(Auth auth) {
    splitValutSecret(auth.getSecret(), auth::setSecretPath, auth::setSecretField);
    splitValutSecret(auth.getClientId(), auth::setClientIdPath, auth::setClientIdField);
  }

  private void splitValutSecret(String secret, Consumer<String> pathConsumer, Consumer<String> fieldConsumer) {
    if (secret != null && !secret.isBlank()) {
      if(secret.contains("lowcode/")) {
        var parts = secret.split("lowcode/");
        if(parts.length > 1) {
          var entry = parts[1].substring(0, parts[1].length() - 1);
          var entryWithPointer = entry.split("->");
          if(entryWithPointer.length > 1) {
            pathConsumer.accept(entryWithPointer[0]);
            fieldConsumer.accept(entryWithPointer[1]);
          }
        }
      } else {
        throw new IllegalStateException("Unexpected secret value:: " + secret);
      }
    }
  }
  
  private boolean requestStop(String businessKey, boolean terminate) {
    try {
      if (terminate) {
        engine.terminate(businessKey, "Terminating by api");
      } else {
        engine.cancel(businessKey);
      }
      return true;
    } catch (WorkflowNotFoundException ex) {
      log.warn("Can't stop the process. Workflow with businessKey = {} not found", businessKey);
      return false;
    }
  }

  @Override
  @Transactional
  public WorkflowDefinition findDraftById(UUID id) {
    return repo.findByIdAndStatusAndDeleted(id, Const.DefinitionStatus.DRAFT, false)
      .orElseThrow(() -> new ClientError(HttpStatus.NOT_FOUND, Errors2.DRAFT_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id})));
  }

  @Override
  @Transactional
  public WorkflowDefinition findDeployedById(UUID id) {
    return repo.findByIdAndStatusAndDeleted(id, Const.DefinitionStatus.DEPLOYED, false)
        .orElseThrow(() -> new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKFLOW_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id})));
  }

  @Override
  public List<DefinitionListValue> searchDefinitions(DefinitionSearching searchConfig) {
    searchConfig.setDefaults();
    return repo.search(searchConfig);
  }

  @Override
  public Long searchDefinitionsCount(DefinitionSearching searchConfig) {
    return repo.searchCount(searchConfig);
  }
  
  @Override
  public WorkflowInstanceSearchResult searchInstances(WorkflowInstanceSearch searchConfig) {
    return engine.searchInstances(searchConfig);
  }

  @Override
  public Long searchInstancesCount(WorkflowInstanceSearch searchConfig) {
    return engine.searchInstancesCount(searchConfig);
  }

  @Override
  public InstanceHistory getInstanceHistory(WorkflowIstanceIdentity identity) {
    return engine.getInstanceHistory(identity);
  }

  @Override
  public void optimizeVariables(Variables variables, BlobSaveOptions blobSaveOptions) {
    var vars = variables.getVars();
    Map<String, JsonNode> toReplace = new HashMap<>();
    vars.forEach((k,v) -> {
      jsonObjectOptimizer.optimizeValue(v, blobSaveOptions).ifPresent(ref -> {
        toReplace.put(k, new TextNode(ref.asLowCodeDecorateVariableRef()));
      });
    });
    vars.putAll(toReplace);
  }


}
