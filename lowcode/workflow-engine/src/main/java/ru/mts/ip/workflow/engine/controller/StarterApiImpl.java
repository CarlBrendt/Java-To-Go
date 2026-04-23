package ru.mts.ip.workflow.engine.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ReqDetachedStarter;
import ru.mts.ip.workflow.engine.controller.dto.ResCount;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.ResStarterShortListValue;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterSearching;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterV2;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.starter.StarterService;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.v1.StarterSearchingSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.StarterSchemaV2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StarterApiImpl implements StarterApi {
  private final ValidationService validationService;
  private final StarterService starterService;
  private final DtoMapper mapper;
  @Override
  public ResReplacedStarter createOrReplaceStarter(String starter) {
    ReqStarterV2 req = validationService.valid(starter, new StarterSchemaV2(Constraint.NOT_NULL, Constraint.NOT_BLANK)
        , ReqStarterV2.class);
   return starterService.createOrReplaceStarter(mapper.toStarter(req));
  }

  @Override
  public ResIdHolder createStarter(String starter) {
    ReqStarterV2 req = validationService.valid(starter, new StarterSchemaV2(Constraint.NOT_NULL)
        , ReqStarterV2.class);
    return new ResIdHolder(starterService.createStarterAndWorkers(mapper.toStarter(req)).getId());
  }

  @Override
  public void replaceStarter(UUID id, String starter) {
    ReqDetachedStarter req = validationService.valid(starter, new StarterSchemaV2(Constraint.NOT_NULL)
        , ReqDetachedStarter.class);
    starterService.replaceStarter(id, mapper.toStarter(req));
  }

  @Override
  public ResStarter getStarter(UUID id) {
    var starter = starterService.getStarter(id);
    return mapper.toResStarter(starter);
  }

  @Override
  public void deleteStarter(UUID id) {
    starterService.softDeleteStarter(id);
  }

  @Override
  public void deleteStarter(ReqStopStarter reqStopStarter) {
    starterService.softDeleteStarter(reqStopStarter);
  }

  @Override
  public List<ResStarterShortListValue> findStarters(String starterSearching) {
    var searching = parseSearchConfigOrDefault(starterSearching);
    var values = starterService.search(searching);
    return mapper.toResStarterListEntries(values);
  }

  private StarterSearching parseSearchConfigOrDefault(String starterSearchingText) {
    return Optional.ofNullable(starterSearchingText)
        .map(text -> validationService.valid(starterSearchingText, new StarterSearchingSchema(Constraint.NOT_NULL), ReqStarterSearching.class))
        .map(mapper::toSearchConfig)
        .orElse(new StarterSearching());
  }

  @Override
  public ResCount findStartersCount(String searchConfig) {
    var searching = parseSearchConfigOrDefault(searchConfig);

    return new ResCount(starterService.searchCount(searching));
  }

  @Override
  public void partialUpdateStarter(UUID id, ReqStarterPatch starterPatch) {
    starterService.updateStarter(id, starterPatch);
  }
}
