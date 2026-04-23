package ru.mts.workflowmail.validation.schema.v1;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowmail.service.dto.MailConsumerForInternal;
import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.Context;
import ru.mts.workflowmail.share.validation.schema.BaseSchema;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;

import static ru.mts.workflowmail.share.validation.Constraint.VALID_JSON_SCHEMA;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_OUTPUT_TEMPLATE_VALUE;
import static ru.mts.workflowmail.share.validation.ValidationHelper.getObjectField;

public class ReqCompatibilityStarterSchema extends ObjectSchema {
  private static final String OUTPUT_TEMPLATE = "outputTemplate";
  private static final String WORKFLOW_INPUT_VALIDATE_SCHEMA = "workflowInputValidateSchema";

  public ReqCompatibilityStarterSchema(Constraint...constraint) {
    super(constraint);
    putField(OUTPUT_TEMPLATE,new BaseSchema(VALID_OUTPUT_TEMPLATE_VALUE));
    putField(WORKFLOW_INPUT_VALIDATE_SCHEMA,new BaseSchema(VALID_JSON_SCHEMA));
  }

  @Override
  protected void enrichContext(Context ctx, JsonNode value) {
    MailConsumerForInternal consumer = new MailConsumerForInternal();
    getObjectField(value, OUTPUT_TEMPLATE).ifPresent(consumer::setOutputTemplate);
    ctx.initScriptExecutionContext(consumer);
  }
}
