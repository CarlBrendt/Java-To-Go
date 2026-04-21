package ru.mts.ip.workflow.engine.validation.schema.v1;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_BASE64_VALUE;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_XSD_SCHEMA;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;

public class XsdValidationSchema extends ObjectSchema {
  public static final String IMPORTS = "imports";
  public static final String IMPORTS_XSD_FILE_NAME = "xsdFileName";
  public static final String IMPORTS_BASE64_FILE_CONTENT = "base64FileContent";
  public static final String VARIABLES_TO_VALIDATE = "variablesToValidate";
  public static final String VARIABLES_TO_VALIDATE_VARIABLE_NAME = "variableName";
  public static final String VARIABLES_TO_VALIDATE_XSD_SCHEMA_BASE64_CONTENT =
      "xsdSchemaBase64Content";

  public XsdValidationSchema(Constraint... constraints) {
    super(constraints);
    putField(IMPORTS, new ArraySchema(new ObjectSchema(
        Map.of(IMPORTS_XSD_FILE_NAME, new StringSchema(FILLED, NOT_BLANK),
            IMPORTS_BASE64_FILE_CONTENT, new StringSchema(FILLED, VALID_BASE64_VALUE)))));
    putField(VARIABLES_TO_VALIDATE, new ArraySchema(new ObjectSchema(
        Map.of(VARIABLES_TO_VALIDATE_VARIABLE_NAME, new StringSchema(FILLED, NOT_BLANK),
            VARIABLES_TO_VALIDATE_XSD_SCHEMA_BASE64_CONTENT,
            new StringSchema(FILLED, VALID_BASE64_VALUE, VALID_XSD_SCHEMA))), FILLED));
  }

  @Override
  protected void enrichContext(Context ctx, JsonNode value) {
    super.enrichContext(ctx, value);
    if (isObject(value)) {
      if (isFilled(value, IMPORTS)) {
        var json = value.get(IMPORTS);
        if (json.isArray()) {
          ctx.setWorkflowInputXsdValidateSchemaImports(json);
        }
      } else if (isFilled(value, VARIABLES_TO_VALIDATE)) {
        var json = value.get(VARIABLES_TO_VALIDATE);
        if (json.isArray()) {
          ctx.setWorkflowInputXsdValidateSchemaVariables(json);
        }
      }
    }
  }
}
