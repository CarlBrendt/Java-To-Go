package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;
import ru.mts.ip.workflow.engine.controller.ActivityExecutionContext;
import ru.mts.ip.workflow.engine.controller.RuntimeWorkflowExpression;
import ru.mts.ip.workflow.engine.dto.DefinitionDetails;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.executor.WorkflowExecutorService;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepositoryHelper;
import ru.mts.ip.workflow.engine.service.XsdService;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;
import ru.mts.ip.workflow.engine.service.starter.StarterValidationService;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.mts.ip.workflow.engine.Const.Errors2.INVALID_BODY_JSON;

@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService{

  @Qualifier("objectMapperDuplicateDetect")
  private final ObjectMapper objectMapper;
  private final ErrorCompiler errorCompiler;
  private final StarterValidationService starterValidationService;
  private final WorkflowDefinitionRepositoryHelper workflowDefinitionRepositoryHelper;
  private final WorkflowExecutorService workflowExecutorService;
  private final EngineConfigurationProperties props;
  private final ScriptExecutorService scriptExecutorService;
  private final XsdService xsdService;

  private static final String SPEL_REGEX = "spel\\{.*?\\}";
  private static final Pattern SPEL_PATTERN = Pattern.compile(SPEL_REGEX);

  @Override
  public ValidationResult validate(String toValidate, BaseSchema schema) {
    schema.initPath(null);
    JsonParseResult parseResult = parse(toValidate);
    var parseErrors = parseResult.getErrors();
    var jsonNode = parseResult.getJson();

    parseErrors.addAll(validateNoSpel(toValidate));

    if(jsonNode != null) {
      var valResult = validate(jsonNode, schema);
      parseErrors.addAll(valResult.getErrors());
    }
    
    return new ValidationResult(jsonNode, parseErrors);
  }

  private List<ErrorDescription> validateNoSpel(String toValidate){
    Matcher matcher = SPEL_PATTERN.matcher(toValidate);
    List<ErrorDescription> errorDescriptions = new ArrayList<>();
    if (matcher.find()) {
      errorDescriptions.add(errorCompiler.error(Const.Errors2.SPEL_IS_NOT_SUPPORTED));
    }
    return errorDescriptions;
  }

  private JsonParseResult parse(String json) {
    JsonParseResult res = new JsonParseResult();
    List<ErrorDescription> errors = new ArrayList<>();
    if(json == null) {
      errors.add(errorCompiler.error(INVALID_BODY_JSON));
    } else {
      try {
        res.setJson(objectMapper.readTree(json));
      } catch (JsonProcessingException ex) {
        errors.add(errorCompiler.error(INVALID_BODY_JSON, ex));
      }
    }
    return res.setErrors(errors);
  }


  @Override
  public <T> T valid(String value, BaseSchema schema, Class<T> clazz, boolean ignoreWarnings) {
    var validationResult = validate(value, schema);
    if(ignoreWarnings) {
      if(validationResult.containCriticalErrors()) {
        throw new ConstraintViolationException(validationResult.getErrors());
      }
    } else {
      if(validationResult.containErrors()){
        throw new ConstraintViolationException(validationResult.getErrors());
      }
    }
    return  validationResult.readValue(objectMapper, clazz).orElseThrow();
  }

  @Override
  public <T> ValidationAndParseResult<T> validateAndParse(String value, BaseSchema schema, Class<T> clazz) {
    var validationResult = validate(value, schema);
    return new ValidationAndParseResult<T>()
        .setValidationResult(validationResult)
        .setParseResult(validationResult.containCriticalErrors() ? null : validationResult.readValue(objectMapper, clazz).orElseThrow());
  }

  @Override
  public <T> T valid(String value, BaseSchema schema, Class<T> clazz) {
    return valid(value, schema, clazz, true);
  }
  
  @Override
  public ValidationResult validate(JsonNode jsonNode, BaseSchema schema) {
    List<ErrorDescription> errors = new ArrayList<>();
    if(jsonNode != null) {
      Context ctx = new Context(objectMapper, starterValidationService, workflowDefinitionRepositoryHelper, workflowExecutorService, props, scriptExecutorService, xsdService);
      schema.validate(ctx, jsonNode);
      List<ErrorDescription> validationErrors = schema.getViolations().stream()
          .map(errorCompiler::toErrorDescription).flatMap(Collection::stream).toList();
      errors.addAll(validationErrors);
    }
    return new ValidationResult(jsonNode, errors);
  }

  @Override
  public ValidationResult validateRuntime(DetailedWorkflowDefinition detailedDefinition) {
    RuntimeWorkflowExpression runtimeExpression = toRuntimeWorkflowExpression(detailedDefinition);
    List<ErrorDescription> errors = workflowExecutorService.runtimeValidateExpression(runtimeExpression).getErrors();
    return new ValidationResult(null, errors);
  }
  
  private RuntimeWorkflowExpression toRuntimeWorkflowExpression(DetailedWorkflowDefinition detailedDefinition) {
    DefinitionDetails details = detailedDefinition.getDetails();
    var schema = Optional.ofNullable(details)
        .map(DefinitionDetails::getInputValidateSchema)
        .filter(node -> node !=null && !node.isEmpty())
        .or(() -> defaultSchemaIfSingleSAPStarter(detailedDefinition))
        .orElse(null);
    var exposedHttpHeaders = Optional.ofNullable(details)
        .map(DefinitionDetails::getExposedHttpHeaders)
        .orElse(null);
    JsonNode expression = detailedDefinition.getCompiled();
    RuntimeWorkflowExpression runtimeExpression  = new RuntimeWorkflowExpression()
        .setExposedHttpHeaders(exposedHttpHeaders)
        .setExpression(expression)
        .setInputValidationSchema(schema);
    if(details != null) {
      runtimeExpression.setSecrets(details.getSecrets());
      runtimeExpression.setXsdValidation(details.getXsdValidation());
    }
    return runtimeExpression;
  }
  
  @SneakyThrows
  private Optional<JsonNode> defaultSchemaIfSingleSAPStarter(DetailedWorkflowDefinition executable) {
    Optional<JsonNode> res = Optional.empty();
    List<Starter> starters = Optional.ofNullable(executable)
      .map(DetailedWorkflowDefinition::getDetails)
      .map(DefinitionDetails::getStarters).orElse(List.of());
    if(starters.size() == 1) {
      Starter s = starters.get(0);
      if(Const.StarterType.SAP_INBOUND.equals(s.getType())) {
        res = Optional.of(objectMapper.readTree(Const.StarterJsonSchema.SAP_INBOUND));
      }
    }
    return res;
  }

  @Override
  public ActivityExecutionContext getActivityExecutionContext(DetailedWorkflowDefinition def, String activityId) {
    var runtimeWorkflowExpression = toRuntimeWorkflowExpression(def);
    return workflowExecutorService.emulateActivityContext(runtimeWorkflowExpression, activityId);
  }

  @Override
  public <T> ValidationAndParseResult<T> validateAndParseIgnoreErrors(String value,
      BaseSchema schema, Class<T> clazz) {
    var validationResult = validate(value, schema);
    return new ValidationAndParseResult<T>()
        .setValidationResult(validationResult)
        .setParseResult(validationResult.readValue(objectMapper, clazz).orElse(null));
  }
  
}
