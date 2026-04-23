package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.STARTER_COMPATIBLE_WITH_DEFINITION;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_JSON_SCHEMA;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class WorkflowDetailsSchema extends ObjectSchema {
  
  public static final String INPUT_VALIDATE_SCHEMA = "inputValidateSchema";
  public static final String XSD_VALIDATION = "xsdValidation";
  public static final String YAML_VALIDATION = "yamlValidation";
  public static final String OUTPUT_VALIDATE_SCHEMA = "outputValidateSchema";
  public static final String INITIAL_APPEND_ACCESS_CONFIG_COMMAND = "initialAppendAccessConfigCommand";
  public static final String STARTERS = "starters";
  public static final String SECRETS = "secrets";
  public static final String EXPOSED_HTTP_HEADERS = "exposedHttpHeaders";
  
  public WorkflowDetailsSchema(String type, Constraint ...constraints) {
    super(constraints);
    putField(INPUT_VALIDATE_SCHEMA, new ObjectSchema(VALID_JSON_SCHEMA));
    putField(OUTPUT_VALIDATE_SCHEMA, new ObjectSchema(VALID_JSON_SCHEMA));
    putField(STARTERS, new ArraySchema(new StarterSchema(NOT_NULL, STARTER_COMPATIBLE_WITH_DEFINITION)));
    putField(SECRETS, new MapSchema(new StringSchema(NOT_NULL, NOT_BLANK)));
    putField(INITIAL_APPEND_ACCESS_CONFIG_COMMAND, new WorkflowAccessListSchema());
    putField(EXPOSED_HTTP_HEADERS, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK)));
    putField(XSD_VALIDATION, new XsdValidationSchema());
    putField(YAML_VALIDATION, new YamlValidationSchema());
  }

  @Override
  protected void enrichContext(Context ctx, JsonNode value) {
    super.enrichContext(ctx, value);
    if(isObject(value)) {
      if(isFilled(value, INPUT_VALIDATE_SCHEMA)) {
        var json = value.get(INPUT_VALIDATE_SCHEMA);
        if(json.isObject()) {
          ctx.setWorkflowInputValidateSchema(json);
        }
      }
    }
  }
  
}
