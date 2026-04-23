package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.KEYS_NOT_EQUAL_ACTIVITY_ID;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.TRANSITION_EXISTS;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class InjectSchema extends ObjectSchema{

  public InjectSchema() {
    putField("injectData", new MapSchema(new BaseSchema(), FILLED, KEYS_NOT_EQUAL_ACTIVITY_ID));
    putField("transition", new StringSchema(NOT_BLANK, TRANSITION_EXISTS));
  }

}
