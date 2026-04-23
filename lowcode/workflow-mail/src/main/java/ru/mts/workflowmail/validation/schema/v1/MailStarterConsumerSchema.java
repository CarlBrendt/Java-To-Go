package ru.mts.workflowmail.validation.schema.v1;

import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;

import static ru.mts.workflowmail.share.validation.Constraint.NOT_NULL;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_JSON_SCHEMA;

public class MailStarterConsumerSchema extends ObjectSchema {

  public final static String CONNECTION = "connectionDef";
  public final static String MAIL_FILTER = "mailFilter";
  public static final String OUTPUT_TEMPLATE = "outputTemplate";


  public MailStarterConsumerSchema(Constraint... constraint) {
    super(constraint);
    putField(CONNECTION, new MailConnectionSchema(NOT_NULL));
    putField(MAIL_FILTER, new MailFilterSchema());
    putField(OUTPUT_TEMPLATE, new ObjectSchema(VALID_JSON_SCHEMA));

  }

}
