package ru.mts.ip.workflow.engine.validation.schema.v1;


import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.WORKFLOW_EXISTS_BY_REF;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.ValidateDecision;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.BooleanSchema;
import ru.mts.ip.workflow.engine.validation.schema.ConstraintViolation;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class StopWorkflowSchema extends ObjectSchema {

  public static final String WORKFLOW_REF = "workflowRef";
  public static final String BUSINESS_KEY = "businessKey";
  public static final String TERMINATE = "terminate";

  public StopWorkflowSchema(Constraint ...constraints) {
    super(constraints);
    putField(WORKFLOW_REF, new RefSchema(NOT_NULL, WORKFLOW_EXISTS_BY_REF));
    putField(BUSINESS_KEY, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(TERMINATE, new BooleanSchema());
  }

  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    validateExistsRefOrBusinessKey(json);
  }

  private void validateExistsRefOrBusinessKey(JsonNode json) {
    if(isObject(json)) {
      if(!isFilled(json, WORKFLOW_REF) && !isFilled(json, BUSINESS_KEY)) {
        ValidateDecision des = new ValidateDecision(Const.Errors2.FILED_NOTHING);
        violations.add(new ConstraintViolation().setError(des.getError())
            .setPath(getPath()).setMessageArgs(new ErrorMessageArgs().and().and(WORKFLOW_REF, BUSINESS_KEY)));
      }
    }
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, BUSINESS_KEY)) {
      removeFields(WORKFLOW_REF);
      return new StopWorkflowWithBusinessKey();
    } else if(isFilled(json, WORKFLOW_REF)) {
      removeFields(BUSINESS_KEY);
      return new StopWorkflowWithRef();
    } else {
      return null;
    }
  }

  static class StopWorkflowWithBusinessKey extends ObjectSchema{
    public StopWorkflowWithBusinessKey(Constraint ...constraints) {
      super(constraints);
      putField(BUSINESS_KEY, new StringSchema(NOT_NULL, NOT_BLANK));
      putField(TERMINATE, new BooleanSchema());
    }
  }

  static class StopWorkflowWithRef extends ObjectSchema{
    public StopWorkflowWithRef(Constraint ...constraints) {
      super(constraints);
      putField(WORKFLOW_REF, new RefSchema(FILLED, NOT_NULL, WORKFLOW_EXISTS_BY_REF));
      putField(TERMINATE, new BooleanSchema());
    }
  }
}
