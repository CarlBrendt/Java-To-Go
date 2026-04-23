package ru.mts.ip.workflow.engine.validation.schema.v2.rabbitmq;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;



public class RabbitmqMessageSchema extends ObjectSchema {

  public static final String PAYLOAD = "payload";
  public static final String QUEUE = "queue";
  public static final String HEADERS = "headers";
  public static final String PROPERTIES = "properties";

  public RabbitmqMessageSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(PAYLOAD, new BaseSchema(FILLED));
    putField(QUEUE, new StringSchema(FILLED));
    putField(HEADERS, new BaseSchema());
    putField(PROPERTIES, new BaseSchema());
  }

}
