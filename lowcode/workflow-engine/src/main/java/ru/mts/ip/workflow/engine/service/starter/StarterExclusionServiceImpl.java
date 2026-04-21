package ru.mts.ip.workflow.engine.service.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.entity.StarterExclusionEntity;
import ru.mts.ip.workflow.engine.repository.StarterExclusionRepository;
import ru.mts.ip.workflow.engine.repository.StarterRepository;
import ru.mts.ip.workflow.engine.service.dto.StarterExclusion;
import ru.mts.ip.workflow.engine.utility.ErrorHelper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StarterExclusionServiceImpl implements StarterExclusionService {
  private final StarterRepository starterRepository;
  private final StarterExclusionRepository exclusionRepository;
  private final DtoMapper mapper;

  @Override
  public void deleteStarterExclusion(UUID id) {

  }

  @Override
  public StarterExclusionEntity createExclusion(StarterExclusion messageExclusion) {
    var starterId = messageExclusion.getStarterId();
    var starter = starterRepository.findById(starterId)
        .orElseThrow(() -> ErrorHelper.starterIsNotFound(starterId));
    StarterExclusionEntity entity = mapper.toExclusionEntity(messageExclusion, starter);
    return exclusionRepository.save(entity);
  }

  @Override
  public List<StarterExclusionEntity> getStarterExclusions(UUID starterId) {
    return exclusionRepository.getByStarterId(starterId);
  }
}
