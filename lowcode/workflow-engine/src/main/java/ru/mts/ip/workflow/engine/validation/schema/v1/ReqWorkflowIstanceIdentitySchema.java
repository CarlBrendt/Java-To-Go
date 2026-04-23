package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class ReqWorkflowIstanceIdentitySchema extends ObjectSchema{
  
  public final static String RUN_ID = "runId";
  public final static String BUSINESS_KEY = "businessKey";

  public ReqWorkflowIstanceIdentitySchema(List<Constraint> constraints) {
    super(constraints);
    putField(RUN_ID, new StringSchema(NOT_NULL, NOT_BLANK, VALID_UUID));
    putField(BUSINESS_KEY, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
  }
  
  public ReqWorkflowIstanceIdentitySchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

}
