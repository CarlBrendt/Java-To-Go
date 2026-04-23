package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_JSON_SCHEMA;

public class YamlValidationSchema extends ObjectSchema {
  public static final String VARIABLES_TO_VALIDATE = "variablesToValidate";
  public static final String VARIABLE_NAME = "variableName";
  public static final String JSON_SCHEMA =
      "jsonSchema";

  public YamlValidationSchema(Constraint... constraints) {
    super(constraints);
    putField(VARIABLES_TO_VALIDATE, new ArraySchema(new ObjectSchema(
        Map.of(VARIABLE_NAME, new StringSchema(FILLED, NOT_BLANK), JSON_SCHEMA,
            new ObjectSchema(FILLED, VALID_JSON_SCHEMA))), FILLED));
  }
}
