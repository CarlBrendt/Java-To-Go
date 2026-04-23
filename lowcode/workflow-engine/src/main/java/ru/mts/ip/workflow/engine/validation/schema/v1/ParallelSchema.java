package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_COMPLETION_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.TRANSITION_EXISTS;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class ParallelSchema extends ObjectSchema{

  public ParallelSchema() {
    putField("branches", new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK, TRANSITION_EXISTS),
        FILLED, NOT_NULL, NOT_EMPTY_ARRAY));
    putField("completionType", new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_COMPLETION_TYPE));
    putField("transition", new StringSchema(NOT_BLANK, TRANSITION_EXISTS));
  }
  
}
