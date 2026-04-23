package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_WORKFLOW_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class WorkFlowShortDefSchema extends ObjectSchema{
  
  public static final String TYPE = "type";
  public static final String DETAILS = "details";
  
  public WorkFlowShortDefSchema(Constraint ...constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_WORKFLOW_TYPE));
    putField(DETAILS, new WorkflowDetailsSchema(null, FILLED, NOT_NULL));
  }
  
  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, TYPE)) {
        var type = json.get(TYPE).asText();
        if(type != null) {
          if(Const.WorkflowType.POSIBLE_VALUES.contains(type)) {
            return new ObjectSchema(Map.of(DETAILS, new WorkflowDetailsSchema(type, FILLED, NOT_NULL)));
          }
        }
      }
    }
    return null;
  }

}
