package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;



public class KafkaMessageSchema extends ObjectSchema {

  public static final String TOPIC = "topic";
  public static final String KEY = "key";
  public static final String PAYLOAD = "payload";
  public static final String HEADERS = "headers";
  
  public KafkaMessageSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(PAYLOAD, new BaseSchema(FILLED));
    putField(KEY, new BaseSchema());
    putField(TOPIC, new BaseSchema());
    putField(HEADERS, new BaseSchema());
  }

}
