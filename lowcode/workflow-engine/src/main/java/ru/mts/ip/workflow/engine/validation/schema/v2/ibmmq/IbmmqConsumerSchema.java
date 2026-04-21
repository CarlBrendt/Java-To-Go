package ru.mts.ip.workflow.engine.validation.schema.v2.ibmmq;


import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_JSON_SCHEMA;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_OUTPUT_TEMPLATE_VALUE;



public class IbmmqConsumerSchema extends ObjectSchema {

  private static final String CONNECTION_DEF = "connectionDef";
  private static final String QUEUE_NAME = "queueName";
  private static final String PAYLOAD_VALIDATE_SCHEMA = "payloadValidateSchema";
  private static final String OUTPUT_TEMPLATE = "outputTemplate";

  public IbmmqConsumerSchema(Constraint... constraints) {
    super(constraints);
    putField(CONNECTION_DEF, new IbmmqConnectionSchema(FILLED, NOT_NULL));
    putField(QUEUE_NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(PAYLOAD_VALIDATE_SCHEMA, new BaseSchema(VALID_JSON_SCHEMA));
    putField(OUTPUT_TEMPLATE, new BaseSchema(VALID_OUTPUT_TEMPLATE_VALUE));
  }
}
