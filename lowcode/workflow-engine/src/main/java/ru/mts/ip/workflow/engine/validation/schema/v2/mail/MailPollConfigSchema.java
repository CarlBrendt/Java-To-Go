package ru.mts.ip.workflow.engine.validation.schema.v2.mail;


import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class MailPollConfigSchema extends ObjectSchema {
  private final static String POLL_DELAY_SECONDS = "pollDelaySeconds";
  private final static String MAX_FETCH_SIZE = "maxFetchSize";

  public MailPollConfigSchema(Constraint... constraints) {
    super(constraints);
    putField(POLL_DELAY_SECONDS, new NumberSchema());
    putField(MAX_FETCH_SIZE, new NumberSchema());
  }
}
