package ru.mts.ip.workflow.engine.service.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.ContextCompilerBeans;
import ru.mts.ip.workflow.engine.Const.StarterTypeBeanValidator;
import ru.mts.ip.workflow.engine.dto.Starter;
import ru.mts.ip.workflow.engine.dto.XsdValidation;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorLocation;
import ru.mts.ip.workflow.engine.exception.ScriptExecutionContext;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.service.scripting.ScriptExecutorService;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;
import ru.mts.ip.workflow.engine.validation.VariableValidator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StarterValidationServiceImpl implements StarterValidationService {
  private final ObjectMapper objectMapper;
  private final ScriptExecutorService scriptExecutorService;
  private final Map<String, ScriptContextCompiler> scriptContextCompilers;
  private final VariableValidator variableValidator;
  private final ErrorCompiler errorCompiler;

  @Override
  public List<ErrorDescription> validateStarterCompatibleWithDefinition(JsonNode json,
      JsonNode workflowInputValidateSchema,
      List<XsdValidation.VariableToValidate> variablesToValidate) {
    List<ClientErrorDescription> errors = List.of();
    if (json != null && json.isObject() && json.has("type")) {
      Starter starter = objectMapper.convertValue(json, Starter.class);
      if(!Const.StarterType.REST_CALL.equals(starter.getType())) {
        ScriptContextCompiler contextCompiler =
            StarterTypeBeanValidator.fromStarterTypeName(starter.getType())
            .map(StarterTypeBeanValidator::getBeanName)
            .map(scriptContextCompilers::get)
            .orElseGet(() -> scriptContextCompilers.get(ContextCompilerBeans.EMPTY));
        
        var variablesRaw = contextCompiler.compileDefaultVariables(starter);
        var outputTemplate = contextCompiler.getOutputTemplate(starter);
        if (outputTemplate != null && !outputTemplate.isNull()) {
          variablesRaw = transformOutput(variablesRaw, outputTemplate).orElse(variablesRaw);
        }
       var variables =  new Variables(variablesRaw);
        errors = variableValidator.validateVariables(workflowInputValidateSchema, variables);
        errors.forEach(err -> err.setLocation(new ErrorLocation().setFieldPath("")));

       var requiredXsdVariables =  Optional.ofNullable(variablesToValidate).orElse(List.of())
            .stream().map(XsdValidation.VariableToValidate::getVariableName).toList();

        errors.addAll(variableValidator.validateStringVariables(requiredXsdVariables, variables));
      }
    }
    return errorCompiler.toErrorDescription(errors);
  }

  private Optional<JsonNode> transformOutput(JsonNode output, JsonNode outputTemplate) {
    return Optional.ofNullable(outputTemplate)
        .filter(template -> !template.isNull())
        .filter(template -> !isEmptyContainer(template))
        .map(template -> scriptExecutorService.resolvePlaceholders(template,
            new ScriptExecutionContext(output, null)));
  }

  private boolean isEmptyContainer(JsonNode template) {
    return (template.isObject() || template.isArray()) && template.isEmpty();
  }

}
