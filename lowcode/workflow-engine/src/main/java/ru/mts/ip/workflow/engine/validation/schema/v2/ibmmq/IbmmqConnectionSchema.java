package ru.mts.ip.workflow.engine.validation.schema.v2.ibmmq;


import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_BOOTSTRAP_ADDRESS;

public class IbmmqConnectionSchema extends ObjectSchema {

  public static final String AUTH_DEF = "authDef";
  public static final String ADDRESSES = "addresses";
  public static final String CCSID = "ccsid";
  public static final String QUEUE_MANAGER = "queueManager";
  public static final String CHANNEL = "channel";
  
  public IbmmqConnectionSchema(Constraint... constraints) {
    super(constraints);
    putField(ADDRESSES, new StringSchema(FILLED, NOT_BLANK, VALID_BOOTSTRAP_ADDRESS));
    putField(CCSID, new NumberSchema(NOT_NULL));
    putField(QUEUE_MANAGER, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CHANNEL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(AUTH_DEF, new IbmmqAuthSchema(NOT_NULL));
  }
  
}
