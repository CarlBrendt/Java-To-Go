package ru.mts.ip.workflow.engine.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.dto.InputValidationContext;
import ru.mts.ip.workflow.engine.dto.InputValidationContext.PropertyViolation;
import ru.mts.ip.workflow.engine.dto.YamlValidation;
import ru.mts.ip.workflow.engine.exception.ClientErrorDescription;
import ru.mts.ip.workflow.engine.service.Variables;
import ru.mts.ip.workflow.engine.validation.schema.stringformat.CustomJsonSchemaFactory;

import static ru.mts.ip.workflow.engine.controller.dto.DtoMapper.objectMapper;

@Service
@RequiredArgsConstructor
public class VariableValidatorImpl implements VariableValidator {

  private final ObjectMapper om = new ObjectMapper();
  private final ObjectMapper yamlMapper = new YAMLMapper();

  @Override
  public List<ClientErrorDescription> validateVariables(JsonNode inputSchema, Variables variables) {
    List<ClientErrorDescription> res = new ArrayList<>();
    if (inputSchema != null) {
      JsonSchemaFactory factory = CustomJsonSchemaFactory.getInstanceV4WithStringFormat();

      JsonSchema inputSchem = factory.getSchema(inputSchema);
      JsonNode jsonNode = om.valueToTree(variables.getVars());
      Set<ValidationMessage> errors = inputSchem.validate(jsonNode);

      if (!errors.isEmpty()) {
        InputValidationContext errorCtx = new InputValidationContext();
        errorCtx.setInputValidateSchema(inputSchema);
        errorCtx.setValidationTarget(variables.asNode());
                                                    
        errors.forEach(err -> {
          PropertyViolation pc = new PropertyViolation();
          String varName = (String) Stream.of(Optional.ofNullable(err.getArguments()).orElse(new Object[0])).findFirst().orElse("?");
          pc.setPropertyName(varName);
          pc.setPropertyRootPath(err.getInstanceLocation().toString());
          pc.setSystemMessage(err.getMessageKey());
          pc.setConstraintType(err.getType());
          pc.setDetails(err.getDetails());
          pc.setConstraintPath(err.getSchemaLocation().toString());
          errorCtx.addError(pc);
        });

        res.add(new ClientErrorDescription(Errors2.INVALID_START_VARIABLES)
          .setInputValidationContext(errorCtx)
          .setAdviceMessageArgs(new Object[]{String.join(", ", variables.getVars().keySet())}));
      }
      
      
    }

    return res;
  }

  @Override
  public List<ClientErrorDescription> validateStringVariables(List<String> requiredVariables, Variables variables) {
    JsonNode jsonNode = variables.asNode();
    DocumentContext context = JsonPath.parse(jsonNode.toString());
    InputValidationContext errorCtx = new InputValidationContext();
    errorCtx.setValidationTarget(jsonNode);
    for (String requiredVariable : requiredVariables) {
      JsonNode varValue = null;
      String systemMessage = null;
      try {
        varValue = objectMapper.convertValue(context.read(requiredVariable), JsonNode.class);
      } catch (InvalidPathException ex){
        systemMessage = ex.getMessage();
      }

      if (varValue == null || varValue.isNull() || !varValue.isTextual()) {
        PropertyViolation pc = new PropertyViolation();
        pc.setPropertyName(requiredVariable);
        pc.setConstraintType("string");
        pc.setSystemMessage(systemMessage);
        errorCtx.addError(pc);
      }
    }

    if (!errorCtx.getPropertyViolations().isEmpty()) {
      return List.of(new ClientErrorDescription(Errors2.PRIMITIVE_PRECONDITION_ERROR)
          .setInputValidationContext(errorCtx)
          .setAdviceMessageArgs(new Object[]{String.join(", ", variables.getVars().keySet())}));
    }
    return List.of();
  }

  @Override
  public List<ClientErrorDescription> validateYamlVariables(JsonNode variables,
      YamlValidation yamlValidation) {
    List<ClientErrorDescription> res = new ArrayList<>();

    if (yamlValidation == null || yamlValidation.isEmpty()) {
      return List.of();
    }

    InputValidationContext errorCtx = new InputValidationContext();

    JsonSchemaFactory factory = CustomJsonSchemaFactory.getInstanceV4WithStringFormat();


    for (YamlValidation.VariableToValidate variableToValidate : yamlValidation.getVariablesToValidate()) {
      var varName = variableToValidate.getVariableName();
      var jsonSchema = variableToValidate.getJsonSchema();

      var varValue = variables.get(varName);

      if (varValue == null || varValue.isNull()) {
        res.add(new ClientErrorDescription(Errors2.FIELD_NOT_FILED).setInputValidationContext(
            rootStringErrorContext(varName, variables)));
      } else if (!varValue.isTextual()) {
          res.add(new ClientErrorDescription(Errors2.FIELD_WRONG_TYPE).setInputValidationContext(
              rootStringErrorContext(varName, variables)));
      } else{

        String systemMessage = null;
        Set<ValidationMessage> errors = new HashSet<>();
        try {
          JsonNode yamlNode = yamlMapper.readTree(varValue.asText());
          var inputSchem = factory.getSchema(jsonSchema);
          errors = inputSchem.validate(yamlNode);
        } catch (JsonProcessingException e) {
          systemMessage = e.getMessage();
          res.add(new ClientErrorDescription(Errors2.INVALID_YAML_STRUCTURE).setSystemMessage(systemMessage).setMessageAargs(new Object[] {varName}));
        }

        errors.forEach(err -> {
          PropertyViolation pc = new PropertyViolation();
          String yamlVarName =
              (String) Stream.of(Optional.ofNullable(err.getArguments()).orElse(new Object[0]))
                  .findFirst()
                  .orElse("?");
          pc.setPropertyName(yamlVarName);
          pc.setPropertyRootPath(err.getInstanceLocation().toString());
          pc.setSystemMessage(err.getMessage());
          pc.setConstraintType(err.getType());
          pc.setDetails(err.getDetails());
          pc.setConstraintPath(err.getSchemaLocation().toString());
          errorCtx.addError(pc);
        });


        if (!errors.isEmpty()) {
          errorCtx.setInputValidateSchema(jsonSchema);
          errorCtx.setValidationTarget(varValue);

          res.add(new ClientErrorDescription(Errors2.YAML_VALIDATION_FAILURE).setMessageAargs(
              new Object[] {varName}).setInputValidationContext(errorCtx));
        }
      }
    }

    return res;
  }

  private InputValidationContext rootStringErrorContext (String varName, JsonNode validationTarget){
    PropertyViolation pc = new PropertyViolation();
    pc.setPropertyName(varName);
    pc.setPropertyRootPath("$");
    pc.setConstraintType("string");

    InputValidationContext errorCtx = new InputValidationContext();
    errorCtx.setValidationTarget(validationTarget);
    errorCtx.addError(pc);
    return errorCtx;
  }
}
