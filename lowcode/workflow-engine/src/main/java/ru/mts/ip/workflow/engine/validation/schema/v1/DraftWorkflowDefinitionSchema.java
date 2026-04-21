package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class DraftWorkflowDefinitionSchema extends ObjectSchema {
  
  public static final String TYPE = "type";
  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String TENANT_ID = "tenantId";
  public static final String DETAILS = "details";
  public static final String COMPILED = "compiled";
  public static final String OWNER_LOGIN = "ownerLogin";
  public static final String FLOW_EDITOR_CONFIG = "flowEditorConfig";
  

  public DraftWorkflowDefinitionSchema(Constraint ...constraints) {
    super(constraints);
    putField(TYPE, new StringSchema());
    putField(NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(OWNER_LOGIN, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(TENANT_ID, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(DESCRIPTION, new StringSchema());
    putField(COMPILED, new ObjectSchema());
    putField(DETAILS, new ObjectSchema());
    putField(FLOW_EDITOR_CONFIG, new ObjectSchema());
  }
  
}
