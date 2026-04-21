package ru.mts.ip.workflow.engine.validation.schema.v2.mail;


import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_JSON_SCHEMA;

public class MailConsumerSchema extends ObjectSchema {

  public final static String CONNECTION = "connectionDef";
  public final static String MAIL_FILTER = "mailFilter";
  public static final String OUTPUT_TEMPLATE = "outputTemplate";
  public static final String POLL_CONFIG = "pollConfig";


  public MailConsumerSchema(Constraint... constraint) {
    super(constraint);
    putField(CONNECTION, new MailConnectionSchema(NOT_NULL));
    putField(MAIL_FILTER, new MailFilterSchema());
    putField(POLL_CONFIG, new MailPollConfigSchema());
    putField(OUTPUT_TEMPLATE, new ObjectSchema(VALID_JSON_SCHEMA));

  }

}
