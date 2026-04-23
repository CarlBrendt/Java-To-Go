package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.DURATION_NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.TRANSITION_EXISTS;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_DURATION;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class TimerSchema extends ObjectSchema{
  
  public static final String F_TRANSITION = "transition";
  public static final String F_TIMER_DURATION = "timerDuration";
  
  public TimerSchema() {
    putField("timerDuration", new StringSchema(FILLED, NOT_NULL, NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE));
    putField("transition", new StringSchema(NOT_BLANK, TRANSITION_EXISTS));
  }

}
