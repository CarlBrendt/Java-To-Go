package ru.mts.workflowscheduler.share.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.springframework.scheduling.support.CronExpression;
import ru.mts.workflowscheduler.exception.ErrorMessageArgs;
import ru.mts.workflowscheduler.service.VariablesJsonSchema;
import ru.mts.workflowscheduler.utility.DateHelper;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static ru.mts.workflowscheduler.share.validation.Errors2.DURATION_INVALID_FORMET;
import static ru.mts.workflowscheduler.share.validation.Errors2.DURATION_NEGATIVE;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_BLANK;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_EMPTY;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_NOT_FILED;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_NULL;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_WRONG_TYPE;
import static ru.mts.workflowscheduler.share.validation.Errors2.FIELD_WRONG_VALUE;
import static ru.mts.workflowscheduler.share.validation.Errors2.NEGATIVE_NOT_ALLOWED;
import static ru.mts.workflowscheduler.share.validation.Errors2.NUMBER_MORE_THAN_ALLOWED;



public interface ValidationHelper {

  ValidateDecision test(Context ctx, JsonNode field);

  static boolean isFiled(JsonNode json, String name) {
    return Optional.ofNullable(json)
        .filter(JsonNode::isObject)
        .map(obj -> obj.get(name))
        .flatMap(Optional::ofNullable).isPresent();
  }

  static boolean isAllFiled(JsonNode json, String... names) {
    boolean res = true;
    for(String name: List.of(names)) {
      res &= isFiled(json, name);
    }
    return res;
  }

  static boolean isObject(JsonNode json) {
    return Optional.ofNullable(json)
        .filter(JsonNode::isObject)
        .flatMap(Optional::ofNullable).isPresent();
  }

  static Optional<JsonNode> getObjectField(JsonNode json, String fieldName) {
    return Optional.ofNullable(json)
        .filter(JsonNode::isObject)
        .map(obj -> obj.get(fieldName))
        .filter(val -> val != null)
        .filter(JsonNode::isObject);

//        .flatMap(Optional::ofNullable).isPresent();
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

  static Validation filled() {
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

  static Validation workflowExists() {
    return (c, v) -> {
      if(v != null) {
        if(!v.isObject()) {
//          if(!c.refExists(v)) {
//          }
        }
      }
      return fine();
    };
  }

  static Validation validJsonSchema() {
    return (c, v) -> {
      if(v != null) {
        if(v.isObject()) {
          try {
            OM.treeToValue(v, VariablesJsonSchema.class);
          } catch (JsonProcessingException ex) {
            return err(Errors2.INVALID_JSON_SCHEMA);
          }
        }
      }
      return fine();
    };
  }

  static Validation executableScript() {
    return (c, v) -> {
      if(v != null) {
        var actualType = v.getNodeType();
        if(actualType == JsonNodeType.STRING) {
          var script = v.asText();
          if(!c.isExecutable(script)) {
            return err(Errors2.SCRIPT_IS_NOT_EXECUTABLE);
          }
        }
      }
      return fine();
    };
  }

  ObjectMapper OM = new ObjectMapper();

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

  static Validation validCurl() {
    return (c, v) -> {
      //TODO
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

}
