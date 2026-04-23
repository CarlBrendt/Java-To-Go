package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class WorkflowAccessListSchema extends ObjectSchema{

  public final static String ACCESS_ENTRIES = "accessEntries";
  
  public WorkflowAccessListSchema(List<Constraint> constraints) {
    super(constraints);
    putField(ACCESS_ENTRIES, new ArraySchema(new AccessEntry(NOT_NULL, NOT_BLANK), FILLED, NOT_NULL, NOT_EMPTY_ARRAY));
  }
  
  public static class AccessEntry extends ObjectSchema {

    public final static String WORKFLOW_ID = "workflowId";
    public final static String OAUTH2_CLIENT_ID = "oauth2ClientId";
    
    public AccessEntry(List<Constraint> constraints) {
      super(constraints);
      putField(WORKFLOW_ID, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, VALID_UUID));
      putField(OAUTH2_CLIENT_ID, new StringSchema());
    }
    
    public AccessEntry(Constraint ...constraints) {
      this(List.of(constraints));
    }
    
    @Override
    public BaseSchema copy() {
      var res = new AccessEntry(copyConstraints());
      getNested().forEach((k, v) -> res.getNested().put(k, v.copy()));
      return res;
    } 
    
  }
  
  
  public WorkflowAccessListSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }
  
}
