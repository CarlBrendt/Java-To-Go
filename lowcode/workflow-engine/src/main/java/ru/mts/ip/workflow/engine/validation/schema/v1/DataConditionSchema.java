package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.TRANSITION_EXISTS;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class DataConditionSchema extends ObjectSchema{

  public DataConditionSchema(Constraint ...constraints) {
    super(constraints);
    putField("id", new StringSchema(NOT_NULL, NOT_BLANK));
    putField("condition", new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField("conditionDescription", new StringSchema());
    putField("successFlowDescription", new StringSchema());
    putField("transition", new StringSchema(NOT_BLANK, TRANSITION_EXISTS));
  }
  
}
