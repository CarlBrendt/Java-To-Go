package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_TRANSFORM_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class TransformConfigSchema extends ObjectSchema {
  
  public static final String TYPE = "type";
  public static final String TARGET = "target";

  public TransformConfigSchema(BaseSchema targetMapSchema, Constraint... constraints){
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_TRANSFORM_TYPE));
    putField(TARGET, new MapSchema(targetMapSchema, FILLED, NOT_NULL, NOT_BLANK));
  }
  
}
