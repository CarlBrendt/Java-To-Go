package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;


import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_JSON_SCHEMA;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_OUTPUT_TEMPLATE_VALUE;


public class KafkaConsumerSchema extends ObjectSchema {

  public static final String CONNECTION_DEF = "connectionDef";
  public static final String TOPIC = "topic";
  public static final String CONSUMER_GROUP_ID = "consumerGroupId";
  public static final String PAYLOAD_VALIDATE_SCHEMA = "payloadValidateSchema";
  public static final String KEY_VALIDATE_SCHEMA = "keyValidateSchema";
  public static final String HEADERS_VALIDATE_SCHEMA = "headersValidateSchema";
  public static final String OUTPUT_TEMPLATE = "outputTemplate";
  
  public KafkaConsumerSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(CONNECTION_DEF, new KafkaConnectionSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(TOPIC, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CONSUMER_GROUP_ID, new StringSchema(NOT_BLANK));
    putField(PAYLOAD_VALIDATE_SCHEMA, new BaseSchema(VALID_JSON_SCHEMA));
    putField(KEY_VALIDATE_SCHEMA, new BaseSchema(VALID_JSON_SCHEMA));
    putField(HEADERS_VALIDATE_SCHEMA, new BaseSchema(VALID_JSON_SCHEMA));
    putField(OUTPUT_TEMPLATE, new BaseSchema(VALID_OUTPUT_TEMPLATE_VALUE));
  }
}
