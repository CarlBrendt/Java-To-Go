package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class StartWorkflowFromStarterSchema extends ObjectSchema {
  public static final String WORKFLOW_START = "startWorkflow";
  public static final String WORKER_IDENTITY = "workerIdentity";

  public StartWorkflowFromStarterSchema(Constraint...constraints) {
    super(constraints);
    putField(WORKFLOW_START, new StartWorkflowSchema(Constraint.NOT_NULL));
    putField(WORKER_IDENTITY, new WorkerIdentitySchema(Constraint.NOT_NULL));
  }
}
