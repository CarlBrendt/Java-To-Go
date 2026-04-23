package ru.mts.ip.workflow.engine.service.starter;

import ru.mts.ip.workflow.engine.entity.StarterExclusionEntity;
import ru.mts.ip.workflow.engine.service.dto.StarterExclusion;

import java.util.List;
import java.util.UUID;

public interface StarterExclusionService {


  void deleteStarterExclusion(UUID id);

  StarterExclusionEntity createExclusion(StarterExclusion messageExclusion);

  List<StarterExclusionEntity> getStarterExclusions(UUID starterId);
}
