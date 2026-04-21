package ru.mts.ip.workflow.engine.service.starter;

import ru.mts.ip.workflow.engine.controller.dto.starter.patch.ReqStarterPatch;
import ru.mts.ip.workflow.engine.entity.StarterEntity;

public interface StarterPatchService {
  void patchStarterEntity(StarterEntity starterEntity, ReqStarterPatch patch);
}
