package ru.mts.ip.workflow.engine.validation.schema.v2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_SASL_MECHANISM;
import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_SASL_PROTOCOL;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;


public class SaslAuthSchema extends ObjectSchema {
  public static final String PROTOCOL = "protocol";
  public static final String MECHANISM = "mechanism";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String TOKEN_URL = "tokenUrl";
  public static final String SSL_DEF = "sslDef";
  
  public SaslAuthSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(PROTOCOL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_PROTOCOL));
    putField(MECHANISM, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_MECHANISM));
    putField(USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(TOKEN_URL, new BaseSchema());
    putField(SSL_DEF, new SslSchema(FILLED, NOT_NULL));
  }
  
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, MECHANISM)) {
      var mechanismJson = json.get(MECHANISM);
      if(mechanismJson.isTextual()) {
        var mechanismText = mechanismJson.asText();
        if(Const.SaslMechanism.OAUTHBEARER.equals(mechanismText)) {
          return new ObjectSchema(Map.of(
              TOKEN_URL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
              PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
              MECHANISM, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_MECHANISM),
              PROTOCOL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_PROTOCOL),
              SSL_DEF, new SslSchema(FILLED, NOT_NULL)
            ));
        } else if (Const.SaslMechanism.SCRAM_SHA_512.equals(mechanismText)) {
          if(isFilled(json, PROTOCOL)) {
            var protocolJson = json.get(PROTOCOL);
            if(protocolJson.isTextual()) {
              var protocolText = protocolJson.asText();
              if(Const.SaslProtocol.SASL_PLAINTEXT.equals(protocolText)) {
                return new ObjectSchema(Map.of(
                    USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                    PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
                    MECHANISM, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_MECHANISM),
                    PROTOCOL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_PROTOCOL),
                    SSL_DEF, new BaseSchema()
                  )); 
              }
            }
          }
        } else {
          return new ObjectSchema(Map.of(
              SSL_DEF, new SslSchema(FILLED, NOT_NULL),
              PROTOCOL, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_PROTOCOL),
              MECHANISM, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SASL_MECHANISM),
              USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
              PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK)
            ));
        }
      }
    }
    return null;
  }
  
}
