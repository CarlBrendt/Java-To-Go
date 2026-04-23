package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class WorkerIdentitySchema extends ObjectSchema {
  public static final String WORKER_ID = "workerId";
  public static final String EXECUTOR_ID = "executorId";

  public WorkerIdentitySchema(Constraint...constraints) {
    super(constraints);
    putField(WORKER_ID, new StringSchema(Constraint.NOT_NULL, Constraint.VALID_UUID));
    putField(EXECUTOR_ID, new StringSchema(Constraint.NOT_NULL, Constraint.NOT_BLANK));
  }
}
