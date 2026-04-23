package ru.mts.ip.workflow.engine.validation.schema.v1.sap;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class InboundStarterSchema extends ObjectSchema {

  public static final String INBOUND_DEF = "inboundDef";
  
  public InboundStarterSchema(Constraint... constraints) {
    super(constraints);
    putField(INBOUND_DEF, new SapStarterSchema(FILLED, NOT_NULL));
  }

}
