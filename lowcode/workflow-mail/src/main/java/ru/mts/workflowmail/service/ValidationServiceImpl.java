package ru.mts.workflowmail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.workflowmail.exception.ConstraintViolationException;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.service.dto.CompatibilityStarter;
import ru.mts.workflowmail.share.validation.Context;
import ru.mts.workflowmail.share.validation.Errors2;
import ru.mts.workflowmail.share.validation.JsonParseResult;
import ru.mts.workflowmail.share.validation.ValidationResult;
import ru.mts.workflowmail.share.validation.schema.BaseSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {
  private final StarterScriptValidationService scriptValidationService;
  private final ObjectMapper objectMapper;
  private final ErrorCompiler errorCompiler;
  private final ScriptExecutorService scriptExecutorService;


  @Override
  public ValidationResult validate(String toValidate, BaseSchema schema){
    schema.initPath(null);
    JsonParseResult parseResult = parse(toValidate);
    var parseErrors = parseResult.getErrors();
    var jsonNode = parseResult.getJson();

    if(jsonNode != null) {
      Context ctx = new Context(scriptValidationService, errorCompiler, scriptExecutorService);
      schema.validate(ctx, jsonNode);
      List<ErrorDescription> validationErrors = schema.getViolations().stream()
          .map(errorCompiler::toErrorDescription).flatMap(Collection::stream).toList();
      parseErrors.addAll(validationErrors);
    }
    return new ValidationResult(jsonNode, parseErrors);
  }

  private JsonParseResult parse(String json) {
    JsonParseResult res = new JsonParseResult();
    List<ErrorDescription> errors = new ArrayList<>();
    if(json == null) {
      errors.add(errorCompiler.error(Errors2.INVALID_BODY_JSON));
    } else {
      try {
        res.setJson(objectMapper.readTree(json));
      } catch (JsonProcessingException ex) {
        errors.add(errorCompiler.error(Errors2.INVALID_BODY_JSON, ex));
      }
    }
    return res.setErrors(errors);
  }

  @Override
  public <T> T valid(String value, BaseSchema schema, Class<T> clazz) {
    return valid(value, schema, clazz, true);
  }

  @Override
  public <T> T valid(String value, BaseSchema schema, Class<T> clazz, boolean ignoreWarnings) {
    var validationResult = validate(value, schema);
    if(ignoreWarnings) {
      if(validationResult.containCriticalErrors()) {
        throw new ConstraintViolationException(validationResult.getCriticalErrors());
      }
    } else {
      if(validationResult.containErrors()){
        throw new ConstraintViolationException(validationResult.getErrors());
      }
    }
    return  validationResult.readValue(objectMapper, clazz).orElseThrow();
  }

  @Override
  public ValidationResult validateStarterCompatibility(CompatibilityStarter starter) {
    //TODO
    return null;
  }
}
