package ru.mts.ip.workflow.engine.controller.dto;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_WORKFLOW_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_WORKFLOW_EXPRESSION_FOR_ESQL_COMPILATION;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.WorkflowType;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.WorkflowDetailsSchema;

public class EsqlCompilationWorkflowDefinitionSchema extends ObjectSchema{
  
  public static final String TYPE = "type";
  public static final String VERSION = "version";
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String TENANT_ID = "tenantId";
  public static final String DETAILS = "details";
  public static final String COMPILED = "compiled";
  public static final String OWNER_LOGIN = "ownerLogin";
  public static final String FLOW_EDITOR_CONFIG = "flowEditorConfig";
  

  public EsqlCompilationWorkflowDefinitionSchema(Constraint ...constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_WORKFLOW_TYPE));
    putField(NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(OWNER_LOGIN, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(DESCRIPTION, new StringSchema());
    putField(TENANT_ID, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(COMPILED, new ObjectSchema());
    putField(DETAILS, new ObjectSchema());
    putField(FLOW_EDITOR_CONFIG, new ObjectSchema());
    putField(ID, new StringSchema(NOT_BLANK, VALID_UUID));
  }
  
  
  
  @Override
  protected void enrichContext(Context ctx, JsonNode value) {
    super.enrichContext(ctx, value);
    if(isObject(value)) {
      if(isFilled(value, TENANT_ID)) {
        var tenentIdJson = value.get(TENANT_ID);
        if(tenentIdJson.isTextual()) {
          var tenentId = tenentIdJson.asText();
          ctx.setTenantId(tenentId);
        }
      }
    }
  }



  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, TYPE)) {
        var type = json.get(TYPE).asText();
        if(WorkflowType.POSIBLE_VALUES.contains(type)) {
          if(Const.WorkflowType.COMPLEX.equals(type)) {
            return new ObjectSchema(Map.of(
                COMPILED, new ObjectSchema(FILLED, VALID_WORKFLOW_EXPRESSION_FOR_ESQL_COMPILATION),
                DETAILS, new WorkflowDetailsSchema(null, NOT_NULL)
            ));
          } else {
            removeField(COMPILED);
            return new ObjectSchema(Map.of(DETAILS, new WorkflowDetailsSchema(null, FILLED, NOT_NULL)));
          }
        }
      }
    }
    return null;
  }
  
}
