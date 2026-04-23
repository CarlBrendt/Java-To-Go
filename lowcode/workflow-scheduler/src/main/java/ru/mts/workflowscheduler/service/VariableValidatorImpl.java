package ru.mts.workflowscheduler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mts.workflowscheduler.share.script.ClientErrorDescription;
import ru.mts.workflowscheduler.service.InputValidationContext.PropertyViolation;
import ru.mts.workflowscheduler.share.validation.Errors2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class VariableValidatorImpl implements JsonValidator {

  private final ObjectMapper om = new ObjectMapper();

  @Override
  public List<ClientErrorDescription> validateVariables(JsonNode json, JsonNode schema) {
    List<ClientErrorDescription> res = new ArrayList<>();
    if (schema != null) {
      JsonSchemaFactory factory =
          JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V4);

      JsonSchema inputSchem = factory.getSchema(schema);
      Set<ValidationMessage> errors = inputSchem.validate(json);

      if(!errors.isEmpty()) {
        InputValidationContext errorCtx = new InputValidationContext();
        errorCtx.setInputValidateSchema(schema);
        errorCtx.setValidationTarget(json);

        errors.forEach(err -> {
          PropertyViolation pc = new PropertyViolation();
          String varName = Stream.of(err.getArguments()).findFirst().orElse("?");
          pc.setPropertyName(varName);
          pc.setPropertyRootPath(err.getPath());
          pc.setSystemMessage(err.getMessage());
          pc.setConstraintType(err.getType());
          pc.setDetails(err.getDetails());
          pc.setConstraintPath(err.getSchemaPath());
          errorCtx.addError(pc);
        });
        res.add(new ClientErrorDescription(Errors2.SCHEDULER_STARTER_INCOMPATIBLE_WITH_WORKFLOW)
          .setInputValidationContext(errorCtx)
          .setAdviceMessageArgs(Stream.of(json.fieldNames()).toList().toArray(Object[]::new)));


      }

    }
    return res;
  }


}
