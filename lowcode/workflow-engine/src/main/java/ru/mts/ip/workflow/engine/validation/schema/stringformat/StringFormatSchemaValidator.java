package ru.mts.ip.workflow.engine.validation.schema.stringformat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.schema.ErrorMessageType;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.Keyword;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.format.BaseFormatJsonValidator;

import java.util.Set;

public class StringFormatSchemaValidator extends BaseFormatJsonValidator implements JsonValidator {
  private final JsonNode schemaNode;
  private final ObjectMapper xmlMapper = new XmlMapper();
  private final ObjectMapper jsonMapper = new ObjectMapper();

  public StringFormatSchemaValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath,
      JsonNode schemaNode, JsonSchema parentSchema, ErrorMessageType errorMessageType,
      Keyword keyword, ValidationContext validationContext) {
    super(schemaLocation, evaluationPath, schemaNode, parentSchema, errorMessageType, keyword,
        validationContext);
    this.schemaNode = schemaNode;
  }

  @Override
  public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode,
      JsonNode schemaNode, JsonNodePath jsonNodePath) {
    if (this.schemaNode != null && this.schemaNode.isTextual()) {
      if (StringFormat.XML.equalsIgnoreCase(this.schemaNode.asText())) {
        try {
          xmlMapper.readTree(jsonNode.asText());
        } catch (JsonProcessingException e) {
          var message = String.format("Invalid XML format: %s", e.getOriginalMessage());
          return Set.of(ValidationMessage.builder()
              .schemaNode(schemaNode)
              .schemaLocation(schemaLocation)
              .message(message)
              .instanceLocation(jsonNodePath)
              .type("stringFormat")
              .code("stringFormat.xml")
              .build());
        }
      } else if (StringFormat.JSON.equalsIgnoreCase(this.schemaNode.asText())) {
        try {
          jsonMapper.readTree(jsonNode.asText());
        } catch (JsonProcessingException e) {
          var message = String.format("Invalid JSON format: %s", e.getOriginalMessage());
          return Set.of(ValidationMessage.builder()
              .schemaNode(schemaNode)
              .schemaLocation(schemaLocation)
              .message(message)
              .instanceLocation(jsonNodePath)
              .type("stringFormat")
              .code("stringFormat.json")
              .build());
        }
      } else {
        return Set.of(ValidationMessage.builder()
            .schemaNode(schemaNode)
            .schemaLocation(schemaLocation)
            .message("Unexpected string format")
            .instanceLocation(jsonNodePath)
            .type("stringFormat")
            .build());
      }
    }
    return Set.of();
  }
}
