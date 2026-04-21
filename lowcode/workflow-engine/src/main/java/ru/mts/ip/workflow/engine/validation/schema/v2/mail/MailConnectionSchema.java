package ru.mts.ip.workflow.engine.validation.schema.v2.mail;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;


import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_MAIL_PROTOCOL;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;



public class MailConnectionSchema extends ObjectSchema {

  public static final String PROTOCOL = "protocol";
  public static final String HOST = "host";
  public static final String PORT = "port";
  public static final String MAIL_AUTH = "mailAuth";

  public MailConnectionSchema(Constraint... constraints) {
    super(constraints);
    putField(PROTOCOL, new StringSchema(NOT_NULL, NOT_BLANK, FILLED, ACCEPTABLE_MAIL_PROTOCOL));
    putField(HOST, new StringSchema(NOT_NULL, NOT_BLANK, FILLED));
    putField(PORT, new NumberSchema(NOT_NULL, FILLED, NOT_NEGATIVE));
    putField(MAIL_AUTH, new MailAuthSchema(FILLED, NOT_NULL, NOT_BLANK));
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if (isFilled(json, PROTOCOL)) {
      var protocolJson = json.get(PROTOCOL);
      if (protocolJson.isTextual()) {
        var protocol = protocolJson.asText();
        if (Const.MailConnectionProtocol.EWS.equals(protocol)) {
          removeField(PORT);
          return new ObjectSchema(Map.of(HOST, new StringSchema(), MAIL_AUTH,
              new MailAuthSchema(FILLED, NOT_NULL, NOT_BLANK)));
        } else if (Const.MailConnectionProtocol.IMAP.equals(protocol)) {
          new ObjectSchema(Map.of(HOST, new StringSchema(NOT_NULL, NOT_BLANK, FILLED), PORT,
              new NumberSchema(NOT_NULL, FILLED, NOT_NEGATIVE), MAIL_AUTH,
              new MailAuthSchema(FILLED, NOT_NULL, NOT_BLANK)));
        }
      }
    }
    return null;
  }
}
