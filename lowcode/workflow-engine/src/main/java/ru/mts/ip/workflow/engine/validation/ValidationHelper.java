package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;
import ru.mts.ip.workflow.engine.json.VariablesJsonSchema;
import ru.mts.ip.workflow.engine.service.FetchedVaultPropertiesResolver;
import ru.mts.ip.workflow.engine.service.SerializeUtils;
import ru.mts.ip.workflow.engine.service.SimpleCrypt;
import ru.mts.ip.workflow.engine.utility.DateHelper;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64.Decoder;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import static ru.mts.ip.workflow.engine.Const.Errors2.DURATION_INVALID_FORMET;
import static ru.mts.ip.workflow.engine.Const.Errors2.DURATION_NEGATIVE;
import static ru.mts.ip.workflow.engine.Const.Errors2.EXECUTION_TIMEOUT_OVERLIMITED;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_BLANK;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_EMPTY;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_NOT_FILED;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_NULL;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_WRONG_TYPE;
import static ru.mts.ip.workflow.engine.Const.Errors2.FIELD_WRONG_VALUE;
import static ru.mts.ip.workflow.engine.Const.Errors2.INVALID_HOST_PORT;
import static ru.mts.ip.workflow.engine.Const.Errors2.INVALID_ISO_OFFSET_DATE_TIME;
import static ru.mts.ip.workflow.engine.Const.Errors2.NEGATIVE_NOT_ALLOWED;
import static ru.mts.ip.workflow.engine.Const.Errors2.NOT_UNIQUE_ACTIVITY_ID;
import static ru.mts.ip.workflow.engine.Const.Errors2.NUMBER_MORE_THAN_ALLOWED;
import static ru.mts.ip.workflow.engine.Const.Errors2.TRANSITION_IS_NOT_FOUND;
import static ru.mts.ip.workflow.engine.Const.Errors2.VARIABLE_HAS_NAME_AS_ACTIVITY;
import static ru.mts.ip.workflow.engine.Const.Errors2.VERSION_WRONG_VALUE;
import static ru.mts.ip.workflow.engine.Const.Errors2.WORKFLOW_IS_NOT_FOUND_BY_REF;
import static ru.mts.ip.workflow.engine.Const.Errors2.WRONG_HTTP_STATUS_CODE;

public interface ValidationHelper {
  
  static boolean isFilled(JsonNode json, String name) {
    return Optional.ofNullable(json)
        .filter(JsonNode::isObject)
        .map(obj -> obj.get(name))
        .flatMap(Optional::ofNullable).isPresent();
  }

  static boolean isObject(JsonNode json) {
    return Optional.ofNullable(json)
        .filter(JsonNode::isObject)
        .flatMap(Optional::ofNullable).isPresent();
  }
  
  static Validation typeOf(JsonNodeType expected) {
    return (c, v) -> {
      if(v != null) {
        var actualType = v.getNodeType();
        if(actualType != JsonNodeType.NULL) {
          if(actualType != expected) {
            return err(FIELD_WRONG_TYPE).setArgs(args(actualType).and(actualType, expected));
          } 
        }
      }
      return fine();
    };
  }

  static Validation filed() {
    return (c, v) -> {
      if(v == null) {
        return err(FIELD_NOT_FILED);
      }
      return fine();
    };
  }

  static Validation notNull() {
    return (c, v) -> {
      if(v != null) {
        if(v.getNodeType() == JsonNodeType.NULL) {
          return err(FIELD_NULL);
        }
      }
      return fine();
    };
  }
  
  static Validation notBlank() {
    return (c, v) -> {
      if(v != null) {
        var actualType = v.getNodeType();
        if(actualType != JsonNodeType.NULL) {
          if(v.isTextual()) {
            var val = v.asText();
            if(val.isBlank()) {
              return err(FIELD_BLANK);
            }
          }
        }
      }
      return fine();
    };
  }

  static Validation notNegative() {
    return (c, v) -> {
      if(v != null) {
        if(v.isNumber()) {
          if(v.asInt() < 0) {
            return err(NEGATIVE_NOT_ALLOWED);
          }
        }
      }
      return fine();
    };
  }

  static Validation maximum(Long limit) {
    return (c, v) -> {
      if(v != null) {
        if(v.isNumber()) {
          if(v.asInt() > limit) {
            return err(NUMBER_MORE_THAN_ALLOWED);
          }
        }
      }
      return fine();
    };
  }

  static Validation validIsoOffsetDateTime() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var textValue = v.asText();
          if(!DateHelper.testISOValidDate(textValue)) {
            return err(INVALID_ISO_OFFSET_DATE_TIME);
          }
        }
      }
      return fine();
    };
  }

  static Validation validNextPageToken() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var textValue = v.asText();
          if(!SerializeUtils.validNextPageToken(textValue)) {
            return err(Errors2.INVALID_PAGE_TOKEN);
          }
        }
      }
      return fine();
    };
  }

  static Validation validEmail() {
    String emailRegex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    Pattern pattern = Pattern.compile(emailRegex);
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var email = v.asText();
          if(!pattern.matcher(email).matches()) {
            return err(Errors2.INVALID_EMAIL);
          }
        }
      }
      return fine();
    };
  }

  static Validation notEmpty() {
    return (c, v) -> {
      if(v != null) {
        var actualType = v.getNodeType();
        if(actualType == JsonNodeType.ARRAY) {
          if(v.isEmpty()) {
            return err(FIELD_EMPTY);
          }
        }
      }
      return fine();
    };
  }
  
  static Validation transitionExists() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var id = v.asText();
          if(!c.contains(id)) {
            return err(TRANSITION_IS_NOT_FOUND);
          }
        }
      }
      return fine();
    };
  }
  
  static Validation uniqueActivityId() {
    return (c, v) -> {
      if(v != null) {
        var actualType = v.getNodeType();
        if(actualType == JsonNodeType.STRING) {
          var id = v.asText();
          if(!c.isUnique(id)) {
            return err(NOT_UNIQUE_ACTIVITY_ID);
          }
        }
      }
      return fine();
    };
  }

  static Validation keysNotEqualActivityId() {
    return (c, v) -> {
      if (v != null) {
        if (v.isObject()) {
          var iterator = v.fieldNames();
          while (iterator.hasNext()) {
            if (c.contains(iterator.next())) {
              return err(VARIABLE_HAS_NAME_AS_ACTIVITY);
            }
          }
        }
      }
      return fine();
    };
  }

  static Validation durationNotNegative() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var duration = v.asText();
          try {
            Duration res = Duration.parse(duration);
            if(!res.isNegative()) {
              return fine();
            } else {
              return err(DURATION_NEGATIVE);
            }
          } catch (DateTimeParseException noop) {}
        }
      }
      return fine();
    };
  }

  static Validation syncStartTimioutLimit() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var duration = v.asText();
          try {
            Duration limit = c.getSyncStartTimoutLimit();
            Duration res = Duration.parse(duration);
            Duration diff = limit.minus(res);
            if(diff.isZero() || !diff.isNegative()) {
              return fine();
            } else {
              return err(EXECUTION_TIMEOUT_OVERLIMITED);
            }
          } catch (DateTimeParseException noop) {}
        }
      }
      return fine();
    };
  }

  static Validation workflowExistsByRef() {
    return (c, v) -> {
      if(v != null) {
        if(v.isObject()) {
          if(!c.workflowExistsByRef(v)) {
            return err(WORKFLOW_IS_NOT_FOUND_BY_REF);
          }
        }
      }
      return fine();
    };
  }

  static Validation workflowExistsByBk() {
    return (c, v) -> {
      if(v != null) {
        if(v.isObject()) {
          if(!c.workflowExistsByRef(v)) {
            return err(WORKFLOW_IS_NOT_FOUND_BY_REF);
          }
        }
      }
      return fine();
    };
  }

  ObjectMapper OM = new ObjectMapper();
  
  static Validation validJsonSchema() {
    return (c, v) -> {
      try {
        OM.treeToValue(v, VariablesJsonSchema.class);
      } catch (JsonProcessingException ex) {
        return err(Errors2.INVALID_JSON_SCHEMA).setArgs(args(ex.getMessage()));
      }
      return fine();
    };
  }

  Decoder base64 = Base64.getDecoder();
  static Validation validBase64Value() {
    return (c, v) -> {
      if(v != null && v.isTextual()) {
        var text = v.asText();
        if(!text.isBlank()) {
          try {
            text = new String(base64.decode(text), StandardCharsets.UTF_8);
          } catch (Exception ex) {
            return err(Errors2.INVALID_BASE64_VALUE).setArgs(args(ex.getMessage()));
          }
        }
      }
      return fine();
    };
  }
  
  
  static Validation validXsdSchema() {
    return (c, v) -> {
      if(v != null && v.isTextual()) {
        var schemaText = v.asText();
        if(!schemaText.isBlank()) {
          try {
            schemaText = new String(base64.decode(schemaText), StandardCharsets.UTF_8);
            c.tryCreteaXsdSchema(schemaText);
          } catch (Exception ex) {
            return err(Errors2.INVALID_XSD_SCHEMA).setArgs(args(ex.getMessage()));
          }
        }
      }
      return fine();
    };
  }

  static Validation validActivity() {
    return (c, v) -> {
      return new ValidateDecision(c.validateActivity(v));
    };
  }

  static Validation validWorkflowExpression() {
    return (c, v) -> {
      return new ValidateDecision(c.validateWorkflowExpression(v));
    };
  }

  static Validation validWorkflowExpressionForEsqlCompilation() {
    return (c, v) -> {
      return new ValidateDecision(c.validateWorkflowExpressionForEsqlCompilation(v));
    };
  }

  static Validation starterCompatibleWithDefinition() {
    return (c, v) -> {
      return new ValidateDecision(c.validateStarterCompatibleWithDefinition(v));
    };
  }

  static Validation validBootstrapAddress() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual() && !c.isExecutable(v.asText()) && !c.isSecretRef(v.asText())) {
          var val = v.asText();
          var servers = List.of(val.split(","));
          boolean allFine = true;
          FetchedVaultPropertiesResolver vaultResolver = new FetchedVaultPropertiesResolver();

          SimpleCrypt sc = new SimpleCrypt();
          for(String server : servers) {
            if(vaultResolver.containsPlaceholder(server)) {
              server = sc.decryp(vaultResolver.findValue(server).orElse(server));
            }
            var parts = server.split(":");
            if(parts.length == 2) {
              var host = parts[0];
              var port = parts[1];
              try {
                int portInt = Integer.valueOf(port);
                allFine &= (!host.isBlank() && portInt > 0);
                continue;
              } catch (NumberFormatException noop) {
              }
            }
            allFine = false;
          }
          if(allFine) {
            return fine();
          } else {
            return err(INVALID_HOST_PORT);
          }
        }
      }
      return fine();
    };
  }

  static Validation validOutputTemplateValue(){
    //TODO
    return (c, v) -> fine();
  }

  static Validation validOffsetDateTime() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var offsetDateTime = v.asText();
          if(!DateHelper.testISOValidDate(offsetDateTime)) {
            return err(Errors2.INVALID_OFFSET_DATE_TIME);
          }
        }
      }
      return fine();
    };
  }

  static Validation validUUID() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var uuidText = v.asText();
          try {
            UUID.fromString(uuidText);
          } catch (IllegalArgumentException noop) {
            return err(Errors2.INVALID_UUID);
          }
        }
      }
      return fine();
    };
  }
  
  static Validation validDuration() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var duration = v.asText();
          try {
            Duration.parse(duration);
            return fine();
          } catch (DateTimeParseException ex) {
            return err(DURATION_INVALID_FORMET);
          }
        }
      }
      return fine();
    };
  }

  static Validation validCronMin() {
    return (c, v) -> {
      if (v != null && v.isTextual()) {
        var min = v.asText();
        var cronExpression = "* %s * * * *".formatted(min);
        boolean isValid =  CronExpression.isValidExpression(cronExpression);
        if(!isValid) {
          return err(Errors2.INVALID_CRON_MINUTES);
        }
      }
      return fine();
    };
  }

  static Validation validCronHour() {
    return (c, v) -> {
      if (v != null && v.isTextual()) {
        var min = v.asText();
        var cronExpression = "* * %s * * *".formatted(min);
        boolean isValid =  CronExpression.isValidExpression(cronExpression);
        if(!isValid) {
          return err(Errors2.INVALID_CRON_HOUR);
        }
      }
      return fine();
    };
  }

  static Validation validCronMonth() {
    return (c, v) -> {
      if (v != null && v.isTextual()) {
        var month = v.asText();
        var cronExpression = "* * * * %s *".formatted(month);
        boolean isValid =  CronExpression.isValidExpression(cronExpression);
        if(!isValid) {
          return err(Errors2.INVALID_CRON_MONTH);
        }
      }
      return fine();
    };
  }

  static Validation validCronDayOfMonth() {
    return (c, v) -> {
      if (v != null && v.isTextual()) {
        var dayOfMonth = v.asText();
        var cronExpression = "* * * %s * *".formatted(dayOfMonth);
        boolean isValid =  CronExpression.isValidExpression(cronExpression);
        if(!isValid) {
          return err(Errors2.INVALID_CRON_DAY_OF_MONTH);
        }
      }
      return fine();
    };
  }

  static Validation validCronDayOfWeek() {
    return (c, v) -> {
      if (v != null && v.isTextual()) {
        var dayOfMonth = v.asText();
        var cronExpression = "* * * * * %s".formatted(dayOfMonth);
        boolean isValid =  CronExpression.isValidExpression(cronExpression);
        if(!isValid) {
          return err(Errors2.INVALID_CRON_DAY_OF_WEEK);
        }
      }
      return fine();
    };
  }

  static Validation anyOf(List<String> expectedAnyOf) {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var val = v.asText();
          if(!expectedAnyOf.contains(val)) {
            return err(FIELD_WRONG_VALUE).setArgs(args().and(String.join(", ", expectedAnyOf)));
          }
        }
      }
      return fine();
    };
  }

  static Validation acceptableVersion() {
    return (c, v) -> {
      if(v != null) {
        if(v.isTextual()) {
          var val = v.asText();
          if("latest".equals(val)) {
            return fine();
          } else {
            try {
              Integer.valueOf(val);
              return fine();
            } catch (NumberFormatException e) {
            }
          }
          return err(VERSION_WRONG_VALUE);
        }
      }
      return fine();
    };
  }

  static Validation acceptableStatusCode() {
    return (c, v) -> {
      if(v != null) {
        if(v.isNumber()) {
          var val = v.asInt();
          HttpStatus resolved = HttpStatus.resolve(val);
          if(resolved != null) {
            return fine();
          } else {
            return err(WRONG_HTTP_STATUS_CODE).setArgs(args(val));
          }
        }
      }
      return fine();
    };
  }

  static Validation required(Errors2 customError) {
    return (c, v) -> Optional.ofNullable(v).map(val -> fine()).orElse(err(customError));
  }

  static Validation required() {
    return (c, v) -> Optional.ofNullable(v).map(val -> fine()).orElse(err(FIELD_NOT_FILED));
  }

  private static ErrorMessageArgs args(Object ...args) {
    return new ErrorMessageArgs().and(args);
  }
  
  static Validation type(JsonNodeType... expectedTypes) {
    return (c, v) -> {
      if(v != null) {
        JsonNodeType actualType = v.getNodeType();
        if(List.of(expectedTypes).contains(actualType)) {
          var expectedType = Stream.of(expectedTypes).filter(t -> t != JsonNodeType.NULL).findFirst().orElseThrow();
          ValidateDecision arr = err(FIELD_WRONG_TYPE).setArgs(args(actualType).and(actualType, expectedType));
          return arr;
        }
      }
      return fine();
    };
  }

  static Validation object() {
    return type(JsonNodeType.OBJECT);
  }

  static Validation array() {
    return type(JsonNodeType.ARRAY);
  }

  static Validation string(){
    return type(JsonNodeType.STRING);
  }

  static Validation notBlank(Errors2 err) {
    return (c, v) -> string(v).filter(String::isBlank).map(g -> err(err)).orElse(fine());
  }
  
  private static ValidateDecision fine() {
    return new ValidateDecision();
  }
  
  private static Optional<String> string(JsonNode json){
    return Optional.ofNullable(json).filter(JsonNode::isTextual).map(JsonNode::asText);
  }

  private static ValidateDecision err(Errors2 error) {
    return new ValidateDecision(error);
  }

}
