package ru.mts.workflowmail.validation.schema.v1;

import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;
import ru.mts.workflowmail.share.validation.schema.StringSchema;

import static ru.mts.workflowmail.share.validation.Constraint.FILLED;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_NULL;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_JSON_SCHEMA;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_UUID;

public class MailStarterSchema extends ObjectSchema {
  public final static String DESCRIPTION = "description";
  public final static String TENANT_ID = "tenantId";
  public final static String CONNECTION = "connectionDef";
  public final static String MAIL_FILTER = "mailFilter";
  public static final String OUTPUT_TEMPLATE = "outputTemplate";
  public static final String WORKFLOW_DEFINITION_TO_START_ID = "workflowDefinitionToStartId";
  public static final String WORKFLOW_INPUT_VALIDATE_SCHEMA = "workflowInputValidateSchema";

  public MailStarterSchema(Constraint ...constraints) {
    super(constraints);
    putField(DESCRIPTION, new StringSchema());
    putField(TENANT_ID, new StringSchema());
    putField(CONNECTION, new MailConnectionSchema(NOT_NULL));
    putField(MAIL_FILTER, new MailFilterSchema());
    putField(OUTPUT_TEMPLATE, new ObjectSchema(VALID_JSON_SCHEMA));
    putField(WORKFLOW_INPUT_VALIDATE_SCHEMA, new ObjectSchema(VALID_JSON_SCHEMA));
    putField(WORKFLOW_DEFINITION_TO_START_ID, new StringSchema(FILLED, NOT_NULL, VALID_UUID));
  }
}
