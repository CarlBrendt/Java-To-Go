package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class SignalMessageSchema extends ObjectSchema {
  
  private static final String BUSINESS_KEY = "businessKey";
  private static final String MESSAGE_NAME = "messageName";
  private static final String VARIABLES = "variables";

  public SignalMessageSchema(Constraint ...constraints) {
    super(constraints);
    putField(BUSINESS_KEY, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(MESSAGE_NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(VARIABLES, new MapSchema(new BaseSchema()));
  }
  
}
