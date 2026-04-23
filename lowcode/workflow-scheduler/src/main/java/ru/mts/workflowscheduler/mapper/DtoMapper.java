package ru.mts.workflowscheduler.mapper;

import org.mapstruct.Mapper;
import ru.mts.workflowscheduler.controller.dto.ReqSchedulerStarter;
import ru.mts.workflowscheduler.controller.dto.ReqStarterSearching;
import ru.mts.workflowscheduler.controller.dto.ResRestTemplateErrorDescription.ResErrorDescription;
import ru.mts.workflowscheduler.controller.dto.ResSchedulerStarter;
import ru.mts.workflowscheduler.controller.dto.Starter;
import ru.mts.workflowscheduler.engine.ReqRef;
import ru.mts.workflowscheduler.entity.Ref;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.service.ScriptExecutorClient;
import ru.mts.workflowscheduler.service.dto.StarterSearching;
import ru.mts.workflowscheduler.share.script.ResolvePlaceholdersExecutionContext;
import ru.mts.workflowscheduler.share.script.ResolvePlaceholdersExecutionResult;
import ru.mts.workflowscheduler.utility.DateHelper;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DtoMapper {
  List<ResErrorDescription> toResErrorDescriptions(List<ErrorDescription> descriptions);

  ResolvePlaceholdersExecutionResult toResolvePlaceholdersExecutionResult(ScriptExecutorClient.ResResolvePlaceholdersExecutionResult resp);

  ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext toResolvePlaceholdersExecutionContext(
      ResolvePlaceholdersExecutionContext evalArgs);
  ReqRef toReqRef(Ref definitionRef);

  StarterSearching toSearchConfig(ReqStarterSearching req);

  default String map(OffsetDateTime value) {
    return DateHelper.asTextISO(value, null);
  }

  Starter toStarterEngine(ReqSchedulerStarter req);

  ResSchedulerStarter toResStarter(Starter starter);
}
