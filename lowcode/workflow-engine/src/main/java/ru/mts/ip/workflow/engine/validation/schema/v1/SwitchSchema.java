package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class SwitchSchema extends ObjectSchema{
  
  public SwitchSchema() {
    putField("dataConditions", new ArraySchema(new DataConditionSchema(NOT_NULL), FILLED, NOT_NULL, NOT_EMPTY_ARRAY));
    putField("defaultCondition", new DefaultTransitionSchema(FILLED, NOT_NULL));
  }
  
}
