package ru.mts.ip.workflow.engine.validation.schema.v2.rabbitmq;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class RabbitmqConnectionSchema extends ObjectSchema {

  public static final String USER_NAME = "userName";
  public static final String USER_PASS = "userPass";
  public static final String ADDRESSES = "addresses";
  public static final String VIRTUAL_HOST = "virtualHost";

  public RabbitmqConnectionSchema(Constraint... constraints) {
    super(constraints);
    putField(USER_NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(USER_PASS, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(ADDRESSES, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK), FILLED, NOT_NULL, NOT_EMPTY_ARRAY));
    putField(VIRTUAL_HOST, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }
  
}
