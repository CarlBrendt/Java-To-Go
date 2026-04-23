package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.TRANSITION_EXISTS;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class DefaultTransitionSchema extends ObjectSchema{

  public DefaultTransitionSchema(Constraint ...constraints) {
    super(constraints);
    putField("transition", new StringSchema(FILLED, NOT_BLANK, TRANSITION_EXISTS));
    putField("conditionDescription", new StringSchema());
  }
  
}
