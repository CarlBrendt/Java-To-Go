package ru.mts.ip.workflow.engine.validation.schema.v2.sap;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class SapConsumerSchema extends ObjectSchema {
  public static final String SERVER_PROPS = "serverProps";
  public static final String DESTINATION_PROPS = "destinationProps";

  public SapConsumerSchema(Constraint... constraints) {
    super(constraints);
    putField(SERVER_PROPS, new ServerPropsSchema(FILLED, NOT_NULL));
    putField(DESTINATION_PROPS, new DestinationPropsSchema(FILLED, NOT_NULL));
  }

}
