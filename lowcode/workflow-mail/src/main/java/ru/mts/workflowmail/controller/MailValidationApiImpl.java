package ru.mts.workflowmail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.mts.workflowmail.controller.dto.ReqCompatibilityStarter;
import ru.mts.workflowmail.controller.dto.ResValidationResult;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.mapper.DtoMapper;
import ru.mts.workflowmail.service.ValidationService;
import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.validation.schema.v1.MailStarterConsumerSchema;
import ru.mts.workflowmail.validation.schema.v1.ReqCompatibilityStarterSchema;

import java.util.List;

import static ru.mts.workflowmail.share.validation.Constraint.NOT_NULL;

@RestController
@RequiredArgsConstructor
public class MailValidationApiImpl implements MailValidationApi {

  private final ValidationService validationService;
  private final DtoMapper mapper;

  @Override
  public ResValidationResult validateStarterConfig(String starterConfig) {
    List<ErrorDescription>
        errors = validationService.validate(starterConfig, new MailStarterConsumerSchema(NOT_NULL)).getErrors();
    return new ResValidationResult().setErrors(mapper.toResErrorDescriptions(errors));
  }

  @Override
  public ResValidationResult validateStarterCompatibility(String req) {
    var consumerReq = validationService
        .valid(req, new ReqCompatibilityStarterSchema(Constraint.NOT_NULL), ReqCompatibilityStarter.class);
    var errors = validationService.validateStarterCompatibility(mapper.toCompatibilityStarter(consumerReq)).getErrors();
    return new ResValidationResult().setErrors(mapper.toResErrorDescriptions(errors));
  }
}
