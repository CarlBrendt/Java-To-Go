package ru.mts.ip.workflow.engine.validation.schema.v1.sap;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.sap.DestinationPropsSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;



public class SapConnectionSchema extends ObjectSchema {
  
  private static final String NAME = "name";
  @Deprecated
  private static final String TENANTID = "tenantId";
  private static final String DESCRIPTION = "description";
  private static final String PROPS = "props";

  public SapConnectionSchema(Constraint... constraints) {
    super(constraints);
    putField(NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(TENANTID, new StringSchema());
    putField(DESCRIPTION, new StringSchema());
    putField(PROPS, new DestinationPropsSchema(FILLED, NOT_NULL));
  }

  
}
