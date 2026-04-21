package ru.mts.ip.workflow.engine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStarterExclusion;
import ru.mts.ip.workflow.engine.controller.dto.ResIdHolder;
import ru.mts.ip.workflow.engine.controller.dto.starter.ResStarterExclusion;
import ru.mts.ip.workflow.engine.service.dto.StarterExclusion;
import ru.mts.ip.workflow.engine.service.starter.StarterExclusionService;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.ValidationService;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StarterExclusionApiImpl implements StarterExclusionApi {
  private final DtoMapper mapper;
  private final ValidationService validationService;
  private final StarterExclusionService starterExclusionService;

  @Override
  public void deleteStarterExclusion(UUID id) {
    starterExclusionService.deleteStarterExclusion(id);
  }

  @Override
  public ResIdHolder createStarterExclusion(String value) {
    ReqStarterExclusion req =
        validationService.valid(value, new ObjectSchema(Constraint.NOT_NULL),
            ReqStarterExclusion.class);
    StarterExclusion messageExclusion = mapper.toStarterExclusion(req);
    var entity = starterExclusionService.createExclusion(messageExclusion);
    return new ResIdHolder(entity.getId());
  }

  @Override
  public List<ResStarterExclusion> getStarterExclusions(UUID starterId) {
    var entities = starterExclusionService.getStarterExclusions(starterId);
    return mapper.toResStarterExclusions(entities);
  }
}
