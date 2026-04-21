package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.WORKFLOW_EXISTS_BY_REF;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.ValidateDecision;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ConstraintViolation;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class WorkflowCallSchema extends ObjectSchema{

  public final static String ARGS = "args";
  public final static String WORKFLOW_REF = "workflowRef";
  public final static String WORKFLOW_DEF = "workflowDef";
  public final static String RETRY_CONFIG = "retryConfig";
  public final static String FAIL_ACTIVITY_RESULT = "failActivityResult";

  public WorkflowCallSchema(Constraint ...constraints) {
    super(constraints);
    putField(ARGS, new MapSchema(new BaseSchema()));
    putField(RETRY_CONFIG, new RetryConfigSchema());
    putField(FAIL_ACTIVITY_RESULT, new FailActivityResultSchema(NOT_NULL));
  }
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, WORKFLOW_REF)){
      return new FiledWorkflowRef();
    } else if (isFilled(json, WORKFLOW_DEF)) {
      return new FiledWorkflowDef();
    } else {
      return null;
    }
  }
  
  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    if(isObject(json)) {
      if(isFilled(json, WORKFLOW_REF) || isFilled(json, WORKFLOW_DEF)) {
      } else {
        ValidateDecision des = new ValidateDecision(Errors2.FILED_NOTHING);
        violations.add(new ConstraintViolation().setError(des.getError())
            .setPath(getPath()).setMessageArgs(new ErrorMessageArgs().and().and(WORKFLOW_REF, WORKFLOW_DEF)));
      }
    }
  }
  
  static class FiledWorkflowRef extends ObjectSchema{
    public FiledWorkflowRef() {
      putField(WORKFLOW_REF, new RefSchema(NOT_NULL, WORKFLOW_EXISTS_BY_REF));
    }
  }

  static class FiledWorkflowDef extends ObjectSchema{
    public FiledWorkflowDef() {
      putField(WORKFLOW_DEF, new WorkFlowShortDefSchema(NOT_NULL));
    }
  }
  
}
