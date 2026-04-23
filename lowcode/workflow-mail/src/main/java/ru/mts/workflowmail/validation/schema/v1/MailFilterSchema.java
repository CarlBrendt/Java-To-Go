package ru.mts.workflowmail.validation.schema.v1;

import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.schema.ArraySchema;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;
import ru.mts.workflowmail.share.validation.schema.StringSchema;

import static ru.mts.workflowmail.share.validation.Constraint.VALID_EMAIL;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_OFFSET_DATE_TIME;

public class MailFilterSchema extends ObjectSchema {
  private final static String SENDERS = "senders";
  private final static String SUBJECTS = "subjects";
  private final static String START_MAIL_DATE_TIME = "startMailDateTime";

  public MailFilterSchema(Constraint... constraints) {
    super(constraints);
    putField(SENDERS, new ArraySchema(new StringSchema(VALID_EMAIL)));
    putField(SUBJECTS, new ArraySchema(new StringSchema()));
    putField(START_MAIL_DATE_TIME, new StringSchema(VALID_OFFSET_DATE_TIME));
  }
}
