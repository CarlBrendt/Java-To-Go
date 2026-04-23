package ru.mts.workflowscheduler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.mts.workflowscheduler.controller.dto.ResValidationResult;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.mapper.DtoMapper;
import ru.mts.workflowscheduler.service.ValidationService;
import ru.mts.workflowscheduler.validation.schema.v1.SchedulerStarterConfigSchema;

import java.util.List;

import static ru.mts.workflowscheduler.share.validation.Constraint.NOT_NULL;

@RestController
@RequiredArgsConstructor
public class SchedulerValidationApiImpl implements SchedulerValidationApi {

  private final ValidationService validationService;
  private final DtoMapper mapper;

  @Override
  public ResValidationResult validateStarterConfig(String starterConfig) {
    List<ErrorDescription>
        errors = validationService.validate(starterConfig, new SchedulerStarterConfigSchema(NOT_NULL)).getErrors();
    return new ResValidationResult().setErrors(mapper.toResErrorDescriptions(errors));
  }
}
