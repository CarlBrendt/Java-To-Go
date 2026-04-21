package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.ValidateDecision;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ConstraintViolation;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class XsltTransformConfigSchema extends ObjectSchema{
  
  public static final String XSLT_TEMPLATE_REF = "xsltTemplateRef";
  public static final String XSLT_TEMPLATE = "xsltTemplate";
  public static final String XSLT_TRANSFORM_TARGET_REF = "xsltTransformTargetRef";
  public static final String XSLT_TRANSFORM_TARGET = "xsltTransformTarget";
  
  public XsltTransformConfigSchema(Constraint ...constraints) {
    super(constraints);
  }
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isFilled(json, XSLT_TEMPLATE_REF)) {
      if(isFilled(json, XSLT_TRANSFORM_TARGET_REF)) {
        return new ObjectSchema(Map.of(
          XSLT_TEMPLATE_REF, new RefSchema(NOT_NULL),
          XSLT_TRANSFORM_TARGET_REF, new RefSchema(NOT_NULL)
        ));
      } else {
        return new ObjectSchema(Map.of(
          XSLT_TEMPLATE_REF, new RefSchema(NOT_NULL),
          XSLT_TRANSFORM_TARGET, new ObjectSchema(NOT_NULL)
        ));
      } 
    } else {
      if(isFilled(json, XSLT_TRANSFORM_TARGET_REF)) {
        return new ObjectSchema(Map.of(
          XSLT_TEMPLATE, new ObjectSchema(NOT_NULL),
          XSLT_TRANSFORM_TARGET_REF, new RefSchema(NOT_NULL)
        ));
      } else {
        return new ObjectSchema(Map.of(
          XSLT_TEMPLATE, new ObjectSchema(NOT_NULL),
          XSLT_TRANSFORM_TARGET, new ObjectSchema(NOT_NULL)
        ));
      }
    }
  }
  
  
  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    if(isObject(json)) {
      if(isFilled(json, XSLT_TEMPLATE_REF) || isFilled(json, XSLT_TEMPLATE)) {
      } else {
        ValidateDecision des = new ValidateDecision(Errors2.FILED_NOTHING);
        violations.add(new ConstraintViolation().setError(des.getError())
            .setPath(getPath()).setMessageArgs(new ErrorMessageArgs().and().and(XSLT_TEMPLATE_REF, XSLT_TEMPLATE)));
      }
      if(isFilled(json, XSLT_TRANSFORM_TARGET_REF) || isFilled(json, XSLT_TRANSFORM_TARGET)) {
      } else {
        ValidateDecision des = new ValidateDecision(Errors2.FILED_NOTHING);
        violations.add(new ConstraintViolation().setError(des.getError())
            .setPath(getPath()).setMessageArgs(new ErrorMessageArgs().and().and(XSLT_TEMPLATE_REF, XSLT_TEMPLATE)));
      }
    }
  }

}
