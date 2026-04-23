package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_WORKER_STATUSES;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class StarterWorkerSchema extends ObjectSchema {
  public static final String RETRY_COUNT = "retryCount";
  public static final String STATUS = "status";

  public StarterWorkerSchema(Constraint...constraints){
    super(constraints);
    putField(RETRY_COUNT, new NumberSchema());
    putField(STATUS, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_WORKER_STATUSES));
  }
}
