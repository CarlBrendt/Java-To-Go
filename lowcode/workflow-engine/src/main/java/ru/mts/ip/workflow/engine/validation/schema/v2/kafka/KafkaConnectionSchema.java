package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_BOOTSTRAP_ADDRESS;


public class KafkaConnectionSchema extends ObjectSchema {
  
  public static final String BOOTSTRAP_SERVERS = "bootstrapServers";
  public static final String AUTH_DEF = "authDef";
  public static final String TAGS = "tags";
  
  public KafkaConnectionSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(BOOTSTRAP_SERVERS, new StringSchema(FILLED, NOT_BLANK, VALID_BOOTSTRAP_ADDRESS));
    putField(AUTH_DEF, new KafkaAuthSchema());
    putField(TAGS, new ArraySchema(new StringSchema(FILLED, NOT_BLANK)));
  }

}
