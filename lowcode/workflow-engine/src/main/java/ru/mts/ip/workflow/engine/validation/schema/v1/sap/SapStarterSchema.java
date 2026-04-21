package ru.mts.ip.workflow.engine.validation.schema.v1.sap;


import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BooleanSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.sap.ServerPropsSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class SapStarterSchema extends ObjectSchema {

  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String ENABLED = "enabled";
  public static final String PROPS = "props";

  public static final String CONNECTION_DEF = "connectionDef";
  
  public SapStarterSchema(Constraint... constraints) {
    super(constraints);
    putField(NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(DESCRIPTION, new StringSchema());
    putField(ENABLED, new BooleanSchema(NOT_NULL));
    putField(PROPS, new ServerPropsSchema(FILLED, NOT_NULL));
    putField(CONNECTION_DEF, new SapConnectionSchema(FILLED, NOT_NULL));
  }
  
}
