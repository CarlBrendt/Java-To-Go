package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_KAFKA_AUTH_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;



public class KafkaAuthSchema extends ObjectSchema {

  public static final String TYPE = "type";
  public static final String SASL = "sasl";
  public static final String TLS = "tls";
  
  public KafkaAuthSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_KAFKA_AUTH_TYPE));
    putField(SASL, new BaseSchema());
    putField(TLS, new BaseSchema());
  }
  
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, TYPE)) {
      var typeJson = json.get(TYPE);
      if(typeJson.isTextual()) {
        var type = typeJson.asText();
        if(Const.KafkaAuthType.TLS.equals(type)) {
          return new ObjectSchema(Map.of(
              TLS, new TlsSchema(FILLED, NOT_NULL)
            ));
        } else if (Const.KafkaAuthType.SASL.equals(type)) {
          return new ObjectSchema(Map.of(
              SASL, new SaslAuthSchema(FILLED, NOT_NULL)
            ));
        } 
      }
    }
    return null;
  }

}
