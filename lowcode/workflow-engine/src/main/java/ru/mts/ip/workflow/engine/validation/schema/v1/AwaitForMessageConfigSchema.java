package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class AwaitForMessageConfigSchema extends ObjectSchema {
  
  public static final String MESSAGE_NAME = "messageName";
  
  public AwaitForMessageConfigSchema(Constraint ...constraints) {
    super(constraints);
    putField(MESSAGE_NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }
  
}
