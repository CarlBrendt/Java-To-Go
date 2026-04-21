package ru.mts.ip.workflow.engine.validation.schema.v2.ibmmq;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_IBMMQ_AUTH_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;


public class IbmmqAuthSchema extends ObjectSchema {

  public static final String TYPE = "type";
  public static final String BASIC = "basic";
  public static final String BASIC_USERNAME = "userName";
  public static final String BASIC_PASSWORD = "password";

  public IbmmqAuthSchema(Constraint... c) {
    super(c);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_IBMMQ_AUTH_TYPE));
    putField(BASIC, new ObjectSchema());
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, TYPE)) {
        JsonNode typeJson = json.get(TYPE);
        if(typeJson.isTextual()) {
          var type = typeJson.asText();
          if(Const.IbmmqAuthType.BASIC.equals(type)) {
            return new ObjectSchema(Map.of(
                BASIC, new ObjectSchema(Map.of(
                    BASIC_USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                    BASIC_PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
                ))
            ));
          }
        }
      }
    }
    return null;
  }


}
