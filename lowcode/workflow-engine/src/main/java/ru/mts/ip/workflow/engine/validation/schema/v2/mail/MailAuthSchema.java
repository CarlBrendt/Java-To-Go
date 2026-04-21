package ru.mts.ip.workflow.engine.validation.schema.v2.mail;



import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class MailAuthSchema extends ObjectSchema {
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String CERTIFICATE = "certificate";

  public MailAuthSchema(Constraint... constraints) {
    super(constraints);
    putField(USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CERTIFICATE, new MailCertificateSchema());
  }
}
