package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListClosedWorkflowExecutionsResponse;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsResponse;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.controller.dto.ReqStartWorkflow.ReqWorkflowStartConfig;
import ru.mts.ip.workflow.engine.controller.dto.ResDefinitionInspection.InspectionApiInfo;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResErrorDescription;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResScriptWorkflowView;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqSapStarterDetails;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterExclusion;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterSearching;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterV2;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResSapStarterDetails;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterExclusion;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterTask;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResWorker;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.EventCorrelation;
import ru.mts.ip.workflow.engine.dto.IntpApi;
import ru.mts.ip.workflow.engine.dto.Ref;
import ru.mts.ip.workflow.engine.dto.SapConnection;
import ru.mts.ip.workflow.engine.dto.SapInboundStarter;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.dto.WorkflowAccessList;
import ru.mts.ip.workflow.engine.entity.SapStarterDetails;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.entity.StarterExclusionEntity;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;
import ru.mts.ip.workflow.engine.entity.WorkerEntity;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.esql.EsqlService.EsqlToLuaTaskState;
import ru.mts.ip.workflow.engine.exception.ErrorContext;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext.ScriptWorkflowView;
import ru.mts.ip.workflow.engine.executor.WorkflowExpressionValidationResult;
import ru.mts.ip.workflow.engine.service.DefinitionListValue;
import ru.mts.ip.workflow.engine.service.DefinitionSearching;
import ru.mts.ip.workflow.engine.service.SerializeUtils;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.WorkflowIstanceIdentity;
import ru.mts.ip.workflow.engine.service.WorkflowStartConfig;
import ru.mts.ip.workflow.engine.service.dto.StarterDto;
import ru.mts.ip.workflow.engine.service.dto.StarterExclusion;
import ru.mts.ip.workflow.engine.service.dto.StarterLogEntry;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;
import ru.mts.ip.workflow.engine.service.dto.StarterTask;
import ru.mts.ip.workflow.engine.service.dto.WorkerDto;
import ru.mts.ip.workflow.engine.service.scripting.ResolvePlaceholdersExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ResolvePlaceholdersExecutionResult;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorClient.ResResolvePlaceholdersExecutionResult;
import ru.mts.ip.workflow.engine.temporal.InstanceHistory;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearch;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchListValue;
import ru.mts.ip.workflow.engine.temporal.WorkflowInstanceSearchResult;
import ru.mts.ip.workflow.engine.utility.DateHelper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DtoMapper {
  ObjectMapper objectMapper = new ObjectMapper();

  Ref toWorkflowRef(ReqRef req);
  Ref toWorkflowRef(ReqStopWorkflow.ReqStopRef req);

  @Mapping(ignore = true, target = "stand")
  ReqRef toReqRef(Ref req);

  WorkflowStartConfig toWorkflowStartConfig(ReqWorkflowStartConfig req);

  default Variables toVariables(Map<String, JsonNode> vars) {
    return new Variables(vars);
  }

  @Mapping(ignore = true, target = "createTime")
  @Mapping(ignore = true, target = "changeTime")
  @Mapping(ignore = true, target = "deleted")
  @Mapping(ignore = true, target = "version")
  @Mapping(ignore = true, target = "status")
  @Mapping(ignore = true, target = "latest")
  @Mapping(ignore = true, target = "availabilityStatus")
  WorkflowDefinition toWorkflowDefinition(ReqCreateWorkflowDefinition req);

  EventCorrelation toSignalWorkflowInstance(ReqMessage req);

  ResWorkflowDefinition toResWorkflowDefinition(WorkflowDefinition def);

  ResExecutableWorkflow toResExecutableWorkflow(DetailedWorkflowDefinition def);

  @Mapping(ignore = true, target = "sendToKafkaConfig")
  @Mapping(ignore = true, target = "awaitForMessageConfig")
  @Mapping(ignore = true, target = "xsltTransformConfig")
  @Mapping(ignore = true, target = "restCallConfig")
  @Mapping(ignore = true, target = "databaseCallConfig")
  @Mapping(ignore = true, target = "sendToSapConfig")
  @Mapping(ignore = true, target = "sendToRabbitmqConfig")
  @Mapping(ignore = true, target = "transformConfig")
  @Mapping(ignore = true, target = "sendToS3Config")
  ResWorkflowDefinition.ResDefinitionDetails toResWorkflowDefinitionDetails(DefinitionDetails def);

  List<ResErrorDescription> toResErrorDescriptions(List<ErrorDescription> errorDescriptions);

  List<ErrorDescription> toErrorDescription(List<ResErrorDescription> errorDescriptions);

  @Mapping(ignore = true, target = "startUrl")
  ResRef toResRef(Ref req);

  @Mapping(ignore = true, target = "compiled")
  @Mapping(ignore = true, target = "details")
  DetailedWorkflowDefinition toExecutableWorkflowDefinition(WorkflowDefinition definition);

  DefinitionSearching toDefinitionSearching(ReqDefinitionSearchingWithPagination req);

  ResDefinitionListValue toResDefinitionListValue(DefinitionListValue value);

  List<ResDefinitionListValue> toResDefinitionListValues(List<DefinitionListValue> values);

  @Mapping(ignore = true, target = "name")
  @Mapping(ignore = true, target = "tenantId")
  @Mapping(ignore = true, target = "description")
  ResWorkflowDefinition.ResSapConnection toResSapConnection(SapConnection sapConnection);

  WorkflowAccessList toReqWorkflowAccessConfiguration(ReqWorkflowAccessList req);

  @Mapping(ignore = true, target = "systemMessage")
  ErrorContext toErrorContext(ResWorkflowDefinitionErrorDescription.ResErrorContext resErrorContext);

  @Mapping(ignore = true, target = "rootFieldPath")
  @Mapping(ignore = true, target = "parentLocation")
  ErrorDescription toErrorDescription(ResErrorDescription resErrorDescription);

  default ResScriptWorkflowView toResScriptWorkflowView(ScriptWorkflowView scriptView) {
    if (scriptView == null) {
      return null;
    } else {
      var res = new ResScriptWorkflowView().setBusinessKey(scriptView.getBusinessKey()).setInitVariables(scriptView.getInitVariables());
      if (scriptView.getSecrets() != null) {
        var secrets = scriptView.getSecrets()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> "*****"));
        res.setSecrets(secrets);
      }
      return res;
    }
  }


  WorkflowInstanceSearch toWorkflowInstanceSearch(ReqWorkflowInstanceSearch req);

  ResWorkflowInstanceSearchResult toResWorkflowInstanceSearchResult(
      WorkflowInstanceSearchResult res);


  default WorkflowInstanceSearchResult toWorkflowInstanceSearchResult(
      ListWorkflowExecutionsResponse resp) {
    var res = new WorkflowInstanceSearchResult();
    res.setNextPageToken(SerializeUtils.encodeNextPageToken(resp.getNextPageToken().toByteArray()));
    var values =
        resp.getExecutionsList().stream().map(this::toWorkflowInstanceSearchListValue).toList();
    res.setValues(values);
    return res;
  }

  default WorkflowInstanceSearchResult toWorkflowInstanceSearchResult(
      ListClosedWorkflowExecutionsResponse resp) {
    var res = new WorkflowInstanceSearchResult();
    res.setNextPageToken(SerializeUtils.encodeNextPageToken(resp.getNextPageToken().toByteArray()));
    var values =
        resp.getExecutionsList().stream().map(this::toWorkflowInstanceSearchListValue).toList();
    res.setValues(values);
    return res;
  }

  default WorkflowInstanceSearchResult toWorkflowInstanceSearchResult(
      ListOpenWorkflowExecutionsResponse resp) {
    var res = new WorkflowInstanceSearchResult();
    res.setNextPageToken(SerializeUtils.encodeNextPageToken(resp.getNextPageToken().toByteArray()));
    var values =
        resp.getExecutionsList().stream().map(this::toWorkflowInstanceSearchListValue).toList();
    res.setValues(values);
    return res;
  }


  default WorkflowInstanceSearchListValue toWorkflowInstanceSearchListValue(
      WorkflowExecutionInfo info) {
    var execution = info.getExecution();
    WorkflowExecutionStatus status = info.getStatus();
    var startTime = info.getStartTime();
    var closetTime = info.getCloseTime();
    var res =
        new WorkflowInstanceSearchListValue().setStartTime(DateHelper.asTextISO(startTime, null))
            .setCloseTime(DateHelper.asTextISO(closetTime, null))
            .setStatus(
                Const.WorkflowInstanceStatus.ofTemporalInternal(status.toString()).orElseThrow())
            .setWorkflowName(info.getType().getName())
            .setRunId(execution.getRunId())
            .setBusinessKey(execution.getWorkflowId());
    var memo = info.getMemo();

    Optional.ofNullable(memo.getFieldsMap().getOrDefault(ReqCreateWorkflowDefinitionSchema.ID, null))
        .flatMap(p -> SerializeUtils.jsonTextToString(p.getData().toByteArray()))
        .ifPresent(res::setDefinitionId);
    Optional.ofNullable(memo.getFieldsMap().getOrDefault(ReqCreateWorkflowDefinitionSchema.VERSION, null))
        .flatMap(p -> SerializeUtils.jsonNumber(p.getData().toByteArray()))
        .ifPresent(res::setWorkflowVersion);
    Optional.ofNullable(memo.getFieldsMap().getOrDefault(ReqCreateWorkflowDefinitionSchema.TENANT_ID, null))
        .flatMap(p -> SerializeUtils.jsonTextToString(p.getData().toByteArray()))
        .ifPresent(res::setTenantId);
    info.getExecution().getWorkflowId();
    return res;
  }
  default String map(OffsetDateTime value) {
    return DateHelper.asTextISO(value, null);
  }


  WorkflowIstanceIdentity toWorkflowIstanceIdentity(ReqWorkflowIstanceIdentity req);

  @Mapping(target = "consumedMessages", ignore = true)
  ResInstanceHistory toResInstanceHistory(InstanceHistory history);
  
  ResWorkflowExpressionValidationResult toResWorkflowExpressionValidationResult(WorkflowExpressionValidationResult dto);

  ReqResolvePlaceholdersExecutionContext toResolvePlaceholdersExecutionContext(ResolvePlaceholdersExecutionContext ctx);
  ResolvePlaceholdersExecutionResult toResolvePlaceholdersExecutionResult(ResResolvePlaceholdersExecutionResult ctx);

  ResWorker toResWorker(WorkerDto workerDto);

  @Mapping(target = "workflowDefinitionToStartId", source = "workflowDefinition.id")
  @Mapping(target = "workflowInputValidateSchema", ignore = true)
  ResWorker.ResInnerStarter toResWorker(StarterEntity starter);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createTime", ignore = true)
  @Mapping(target = "changeTime", ignore = true)
  @Mapping(target = "worker", ignore = true)
  @Mapping(target = "workflowInputValidateSchema", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "desiredStatus", ignore = true)
  @Mapping(target = "actualStatus", ignore = true)
  Starter toStarter(ReqDetachedStarter req);

  @Mapping(target = "workflowDefinitionToStartId", source = "workflowDefinition.id")
  @Mapping(target = "workflowInputValidateSchema", ignore = true)
  @Mapping(target = "tenantId", source = "workflowDefinition.tenantId")
  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "worker", ignore = true) // handle in  after mapping
  @Named("toStarterWithContext")
  Starter toStarter(StarterEntity req);

  @Mapping(target = "starter", ignore = true)
  WorkerDto toWorkerDto(WorkerEntity worker);

  @AfterMapping
  default void afterMappingStarterEntity(@MappingTarget Starter starter, StarterEntity entity) {
    if (entity != null && entity.getWorker() != null) {
      WorkerDto workerDto = toWorkerDto(entity.getWorker());
      starter.setWorker(workerDto);
    }
  }

  @Mapping(source = "starter.type", target = "type")
  @Mapping(source = "starter.name", target = "name")
  @Mapping(source = "starter.description", target = "description")
  @Mapping(source = "starter.createTime", target = "createTime")
  @Mapping(source = "starter.changeTime", target = "changeTime")
  @Mapping(source = "starter.id", target = "id")
  @Mapping(source = "workflowDefinition.tenantId", target = "tenantId")
  @Mapping(source = "workflowDefinition", target = "workflowDefinition")
  @Mapping(target = "worker", ignore = true)
  @Mapping(target = "desiredStatus", ignore = true)
  @Mapping(target = "actualStatus", ignore = true)
  @Mapping(target = "exclusions", ignore = true)
  StarterEntity toStarterEntity(Starter starter, WorkflowDefinition workflowDefinition);

  @Mapping(source = "serverProps", target = "inboundDef.props")
  @Mapping(source = "destinationProps", target = "inboundDef.connectionDef.props")
  @Mapping(ignore = true, target = "inboundRef")
  SapInboundStarter toSapInboundStarter(ReqSapStarterDetails req);

  @Mapping(target = "serverProps", source = "inboundDef.props")
  @Mapping(target = "destinationProps", source = "inboundDef.connectionDef.props")
  ResSapStarterDetails toResSapStarterDetails(SapInboundStarter sapInboundStarter);

  @Mapping(target = "serverProps", source = "inboundDef.props")
  @Mapping(target = "destinationProps", source = "inboundDef.connectionDef.props")
  SapStarterDetails toSapStarterDetails(SapInboundStarter sapInboundStarter);

  @Mapping(source = "serverProps", target = "inboundDef.props")
  @Mapping(source = "destinationProps", target = "inboundDef.connectionDef.props")
  @Mapping(ignore = true, target = "inboundRef")
  SapInboundStarter toSapInboundStarter(SapStarterDetails sapInboundStarter);

  List<ResStarterShortListValue> toResStarterListEntries(List<StarterShortListValue> values);

  StarterSearching toSearchConfig( ReqStarterSearching reqStarterSearching);

  List<WorkerDto> toWorkersDto(List<WorkerEntity> workers);

  ResStarter toResStarter(Starter starter);

  StarterExclusion toStarterExclusion(ReqStarterExclusion req);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createTime", ignore = true)
  StarterExclusionEntity toExclusionEntity(StarterExclusion messageExclusion, StarterEntity starter);


  List<ResStarterExclusion> toResStarterExclusions(List<StarterExclusionEntity> entities);

  @Mapping(target = "starterId", source = "starter.id")
  ResStarterExclusion toResStarterExclusion(StarterExclusionEntity starterExclusion);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createTime", ignore = true)
  @Mapping(target = "changeTime", ignore = true)
  @Mapping(target = "tags", ignore = true)
  @Mapping(target = "workflowInputValidateSchema", ignore = true)
  @Mapping(target = "worker", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "desiredStatus", ignore = true)
  @Mapping(target = "actualStatus", ignore = true)
  Starter toStarter(ReqStarterV2 req);

  default JsonNode map(Object value){
    return value == null ? null : objectMapper.valueToTree(value);
  }

  @Mapping(target = "tenantId", source = "workflowDefinition.tenantId")
  @Mapping(target = "workflowDefinitionToStartId", source = "workflowDefinition.id")
  StarterLogEntry toStarterLogEntry(StarterEntity starterEntity);

  StarterTask toStarterTask(ReqStarterTask req);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "error", ignore = true)
  @Mapping(target = "type", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "retryCount", ignore = true)
  @Mapping(target = "lockedUntilTime", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "overdueTime", ignore = true)
  @Mapping(target = "createTime", ignore = true)
  @Mapping(target = "workflowDefinition", ignore = true)
  //@Mapping(target = "skip", ignore = true)
  StarterTaskEntity toStarterTaskEntity(StarterTask starterTask);

  @Mapping(target = "tenantId", source = "workflowDefinition.tenantId")
  @Mapping(target = "workflowDefinitionToStartId", source = "workflowDefinition.id")
  StarterDto toStarterDto(StarterEntity starter);

  ReqResolvePlaceholdersExecutionContext toReqResolvePlaceholdersExecutionContext(ReqResolvePlaceholdersExecutionContext request);
  
  @Mapping(target = "stand", source = "api.stand.id")
  @Mapping(target = "auth.secretPath", ignore = true)
  @Mapping(target = "auth.secretField", ignore = true)
  InspectionApiInfo toInspectionApiInfo(IntpApi api);

  @Mapping(target = "workflowDefinitionToStartId", source = "workflowDefinition.id")
  ResStarterTask toResStarterTask(StarterTaskEntity entity);
  
  ResEsqlToLuaTaskState toResEsqlToLuaTaskState(EsqlToLuaTaskState state);
}
