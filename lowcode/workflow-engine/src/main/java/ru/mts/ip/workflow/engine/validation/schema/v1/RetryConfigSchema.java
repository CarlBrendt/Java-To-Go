package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.DURATION_NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_DURATION;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class RetryConfigSchema extends ObjectSchema {
  public final static String INITIAL_INTERVAL = "initialInterval";
  public final static String MAX_INTERVAL = "maxInterval";
  public final static String MAX_ATTEMPTS = "maxAttempts";
  public final static String BACKOFF_COEFFICIENT = "backoffCoefficient";

  public RetryConfigSchema(Constraint...constraints) {
    super(constraints);
    putField(INITIAL_INTERVAL, new StringSchema(NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE));
    putField(MAX_INTERVAL, new StringSchema(NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE));
    putField(MAX_ATTEMPTS, new NumberSchema(NOT_NEGATIVE));
    putField(BACKOFF_COEFFICIENT, new NumberSchema(NOT_NEGATIVE));
  }
}
