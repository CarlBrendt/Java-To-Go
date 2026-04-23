package ru.mts.workflowmail.validation.schema.v1;

import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;
import ru.mts.workflowmail.share.validation.schema.StringSchema;

import static ru.mts.workflowmail.share.validation.Constraint.FILLED;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_BLANK;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_NULL;

public class MailAuthSchema extends ObjectSchema {
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";

  public MailAuthSchema(Constraint... constraints) {
    super(constraints);
    putField(USERNAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(PASSWORD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }
}
