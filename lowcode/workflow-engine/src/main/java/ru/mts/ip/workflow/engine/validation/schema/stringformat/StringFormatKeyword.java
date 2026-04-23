package ru.mts.ip.workflow.engine.validation.schema.stringformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.CustomErrorMessageType;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.Keyword;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;

public class StringFormatKeyword implements Keyword {

  @Override
  public String getValue() {
    return "stringFormat";
  }

  @Override
  public JsonValidator newValidator(SchemaLocation schemaLocation, JsonNodePath jsonNodePath,
      JsonNode schemaNode, JsonSchema jsonSchema, ValidationContext validationContext)
      throws JsonSchemaException, Exception {

     return new StringFormatSchemaValidator(schemaLocation, jsonNodePath, schemaNode ,jsonSchema, CustomErrorMessageType.of("stringFormatErr"), this, validationContext);
  }
}
