package ru.mts.ip.workflow.engine.controller.dto;

import static ru.mts.ip.workflow.engine.validation.Constraint.DURATION_NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_DURATION;
import static ru.mts.ip.workflow.engine.validation.Constraint.WORKFLOW_EXISTS_BY_REF;
import static ru.mts.ip.workflow.engine.validation.Constraint.DURATION_SYNC_START_TIMIOUT_LIMIT;
import java.util.Map;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;
import ru.mts.ip.workflow.engine.validation.schema.v1.RefSchema;

public class ReqStartWorkflowSyncSchema extends ObjectSchema {
  
  public static final String WORKFLOW_REF = "workflowRef";
  public static final String WORKFLOW_START_CONFIG = "workflowStartConfig";
  
  public static final String BUSINESS_KEY = "businessKey";
  public static final String VARIABLES = "variables";
  public static final String EXECUTION_TIMEOUT = "executionTimeout";
  
  public ReqStartWorkflowSyncSchema(Constraint ...constraints) {
    super(constraints);
    putField(WORKFLOW_REF, new RefSchema(FILLED, NOT_NULL, WORKFLOW_EXISTS_BY_REF));
    putField(WORKFLOW_START_CONFIG, new ObjectSchema(Map.of(
        
      BUSINESS_KEY, new StringSchema(FILLED, NOT_NULL, NOT_BLANK),
      VARIABLES, new MapSchema(new BaseSchema(), NOT_NULL),
      EXECUTION_TIMEOUT, new StringSchema(NOT_NULL, NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE, DURATION_SYNC_START_TIMIOUT_LIMIT)
      
    ), FILLED, NOT_NULL));
  }

}
