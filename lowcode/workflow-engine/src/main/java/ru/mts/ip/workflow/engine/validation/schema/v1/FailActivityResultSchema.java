package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_RETRY_STATES;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class FailActivityResultSchema extends ObjectSchema {
  public final static String RETRY_STATES = "retryStates";
  public final static String VARIABLES = "variables";


  public FailActivityResultSchema(Constraint... constraints) {
    super(constraints);
    putField(RETRY_STATES, new ArraySchema(new StringSchema(NOT_NULL, ACCEPTABLE_RETRY_STATES), NOT_NULL, NOT_EMPTY_ARRAY,
        FILLED));
    putField(VARIABLES, new MapSchema(new BaseSchema(), NOT_NULL));
  }
}
