package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
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
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class RefSchema extends ObjectSchema{
  
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String VERSION = "version";
  private static final String STAND = "stand";
  private static final String TENANT_ID = "tenantId";

  public RefSchema(Constraint ...constraints) {
    super(constraints);
    putField(ID, new StringSchema());
    putField(NAME, new StringSchema());
    putField(VERSION, new StringSchema());
    putField(STAND, new StringSchema());
    putField(TENANT_ID, new StringSchema());
  }
  
  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    validateExistsIdOrName(ctx, json);
  }
  
  private void validateExistsIdOrName(Context ctx, JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, ID) || isFilled(json, NAME)) {
      } else {
        ValidateDecision des = new ValidateDecision(Errors2.FILED_NOTHING);
        violations.add(new ConstraintViolation().setError(des.getError())
            .setPath(getPath()).setMessageArgs(new ErrorMessageArgs().and().and(ID, NAME)));
      }
    }
  }
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, ID)) {
      removeFields(NAME, VERSION, STAND, TENANT_ID);
      return new RefWithIdFiled();
    } else if(isFilled(json, NAME)) {
      removeFields(ID);
      return new RefWithoutIdFiled();
    } else {
      return null;
    }
  }

  static class RefWithIdFiled extends ObjectSchema{
    public RefWithIdFiled(Constraint ...constraints) {
      super(constraints);
      putField(ID, new StringSchema(NOT_NULL, NOT_BLANK, VALID_UUID));
    }
  }

  static class RefWithoutIdFiled extends ObjectSchema{
    public RefWithoutIdFiled() {
      putField(NAME, new StringSchema(NOT_BLANK));
      putField(VERSION, new StringSchema(NOT_BLANK));
      putField(STAND, new StringSchema(NOT_BLANK));
      putField(TENANT_ID, new StringSchema(NOT_BLANK));
    }
  }
  
}
