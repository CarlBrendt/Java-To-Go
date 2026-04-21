package ru.mts.ip.workflow.engine.validation.schema.v2.ibmmq;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;



public class IbmmqMessageSchema extends ObjectSchema {

  public static final String PAYLOAD = "payload";
  public static final String QUEUE = "queue";
  public static final String TOPIC = "topic";
  public static final String PROPERTIES = "properties";

  public IbmmqMessageSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(PAYLOAD, new BaseSchema(FILLED));
    putField(QUEUE, new StringSchema(FILLED));
    putField(QUEUE, new StringSchema(FILLED));
    putField(PROPERTIES, new BaseSchema());
  }

}
