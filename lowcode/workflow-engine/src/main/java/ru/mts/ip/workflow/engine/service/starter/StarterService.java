package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.controller.dto.ResReplacedStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.entity.StarterEntity;
import ru.mts.ip.workflow.engine.service.dto.StarterSearching;
import ru.mts.ip.workflow.engine.service.dto.StarterShortListValue;

import java.util.List;
import java.util.UUID;

public interface StarterService {

  ResReplacedStarter createOrReplaceStarter(Starter starter);

  List<StarterShortListValue> search(StarterSearching searching);

  void applySecrets(Starter starter);

  StarterEntity createStarterAndWorkers(Starter starter);

  void replaceStarter(UUID id, Starter starter);

  Starter getStarter(UUID id);

  void softDeleteStarter(UUID id);
  void softDeleteStarterByWorkflowDefId(UUID id);

  void softDeleteStarter(ReqStopStarter reqStopStarter);

  Long searchCount(StarterSearching searchConfig);

  void disableExpiredStarters();

  void updateStarter(UUID id, ReqStarterPatch patchNode);
}
