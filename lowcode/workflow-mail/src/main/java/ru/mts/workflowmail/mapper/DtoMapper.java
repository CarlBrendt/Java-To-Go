package ru.mts.workflowmail.mapper;

import org.mapstruct.Mapper;
import ru.mts.workflowmail.controller.dto.ReqCompatibilityStarter;
import ru.mts.workflowmail.controller.dto.ResRestTemplateErrorDescription.ResErrorDescription;
import ru.mts.workflowmail.engine.ReqRef;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.service.ScriptExecutorClient;
import ru.mts.workflowmail.service.dto.CompatibilityStarter;
import ru.mts.workflowmail.service.dto.Ref;
import ru.mts.workflowmail.share.script.ResolvePlaceholdersExecutionContext;
import ru.mts.workflowmail.share.script.ResolvePlaceholdersExecutionResult;
import ru.mts.workflowmail.utility.DateHelper;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DtoMapper {
  List<ResErrorDescription> toResErrorDescriptions(List<ErrorDescription> descriptions);

  ResolvePlaceholdersExecutionResult toResolvePlaceholdersExecutionResult(ScriptExecutorClient.ResResolvePlaceholdersExecutionResult resp);

  ScriptExecutorClient.ReqResolvePlaceholdersExecutionContext toResolvePlaceholdersExecutionContext(
      ResolvePlaceholdersExecutionContext evalArgs);

  ReqRef toReqRef(Ref definitionRef);

  CompatibilityStarter toCompatibilityStarter(ReqCompatibilityStarter consumerReq);

  default String map(OffsetDateTime value) {
    return DateHelper.asTextISO(value, null);
  }

}
