package ru.mts.ip.workflow.engine.validation.schema.v2;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.ibmmq.IbmmqConsumerSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.kafka.KafkaConsumerSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.mail.MailConsumerSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.rabbitmq.RabbitmqConsumerSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.sap.SapConsumerSchema;
import ru.mts.ip.workflow.engine.validation.schema.v2.scheduler.SchedulerDetailsSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_STARTER_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_OFFSET_DATE_TIME;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;

public class StarterSchemaV2 extends ObjectSchema{

  public final static String TYPE = "type";
  public final static String NAME = "name";
  public final static String DESCRIPTION = "description";
  public final static String WORKFLOW_DEFINITION_TO_START_ID = "workflowDefinitionToStartId";
  public final static String START_DATE_TIME = "startDateTime";
  public final static String END_DATE_TIME = "endDateTime";
  public final static String SAP_INBOUND_STARTER = "sapInbound";
  public final static String REST_CALL_STARTER = "restCall";
  public final static String SCHEDULER_STARTER = "scheduler";
  public final static String TAGS = "tags";
  public final static String KAFKA_CONSUMER = "kafkaConsumer";
  public final static String RABBITMQ_CONSUMER = "rabbitmqConsumer";
  public final static String MAIL_CONSUMER = "mailConsumer";
  public final static String IBMMQ_CONSUMER = "ibmmqConsumer";

  public StarterSchemaV2(List<Constraint> constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(NOT_NULL, FILLED, NOT_BLANK, ACCEPTABLE_STARTER_TYPE));
    putField(NAME, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(WORKFLOW_DEFINITION_TO_START_ID, new StringSchema(NOT_NULL, NOT_BLANK, VALID_UUID));
    putField(DESCRIPTION, new StringSchema());
    putField(START_DATE_TIME, new StringSchema(VALID_OFFSET_DATE_TIME));
    putField(END_DATE_TIME, new StringSchema(VALID_OFFSET_DATE_TIME));
    putField(SAP_INBOUND_STARTER, new ObjectSchema());
    putField(KAFKA_CONSUMER, new ObjectSchema());
    putField(REST_CALL_STARTER, new ObjectSchema());
    putField(SCHEDULER_STARTER, new ObjectSchema());
    putField(RABBITMQ_CONSUMER, new ObjectSchema());
    putField(MAIL_CONSUMER, new ObjectSchema());
    putField(IBMMQ_CONSUMER, new ObjectSchema());
    putField(TAGS, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK)));
  }
  
  
  public StarterSchemaV2(Constraint ...constraints) {
    this(List.of(constraints));
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, TYPE)) {
        var typeJson = json.get(TYPE);
        if(typeJson.isTextual()) {
          String type = typeJson.asText();
          switch(type) {
            case Const.StarterType.REST_CALL:
              return new ObjectSchema(Map.of(
                REST_CALL_STARTER, new ObjectSchema(NOT_NULL)
              ));
            case Const.StarterType.SAP_INBOUND:
              return new ObjectSchema(Map.of(
                  SAP_INBOUND_STARTER, new SapConsumerSchema(FILLED),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
            case Const.StarterType.SCHEDULER:
              return new ObjectSchema(Map.of(
                  SCHEDULER_STARTER, new SchedulerDetailsSchema(FILLED, NOT_NULL),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
            case Const.StarterType.KAFKA_CONSUMER:
              return new ObjectSchema(Map.of(
                  KAFKA_CONSUMER, new KafkaConsumerSchema(NOT_NULL, FILLED),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
            case Const.StarterType.RABBITMQ_CONSUMER:
              return new ObjectSchema(Map.of(
                  RABBITMQ_CONSUMER, new RabbitmqConsumerSchema(NOT_NULL, FILLED),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
            case Const.StarterType.MAIL_CONSUMER:
              return new ObjectSchema(Map.of(
                  MAIL_CONSUMER, new MailConsumerSchema(NOT_NULL, FILLED),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
            case Const.StarterType.IBMMQ_CONSUMER:
              return new ObjectSchema(Map.of(
                  IBMMQ_CONSUMER, new IbmmqConsumerSchema(NOT_NULL, FILLED),
                  NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                  TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
              ));
          }
        }
      }
    }
    return null;
  }

  @Override
  public BaseSchema copy() {
    var res = new StarterSchemaV2(copyConstraints());
    getNested().forEach((k, v) -> res.getNested().put(k, v.copy()));
    return res;
  } 
  
  

}
