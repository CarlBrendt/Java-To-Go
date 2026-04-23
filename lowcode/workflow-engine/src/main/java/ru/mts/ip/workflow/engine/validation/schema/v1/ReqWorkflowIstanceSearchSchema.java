package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_WORKFLOW_ISNANCE_STATUS;
import static ru.mts.ip.workflow.engine.validation.Constraint.MAX_100;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_ISO_OFFSET_DATE_TIME;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class ReqWorkflowIstanceSearchSchema extends ObjectSchema{
  
  public final static String PAGE_TOKEN = "pageToken";
  public final static String PAGE_SIZE = "pageSize";
  public final static String STARTING_TIME_FROM = "startingTimeFrom";
  public final static String STARTING_TIME_TO = "startingTimeTo";
  public final static String WORKFLOW_NAME = "workflowName";
  public final static String BUSINESS_KEY = "businessKey";
  public final static String EXECUTION_STATUSES = "executionStatus";

  public ReqWorkflowIstanceSearchSchema(List<Constraint> constraints) {
    super(constraints);
    putField(WORKFLOW_NAME, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(PAGE_TOKEN, new StringSchema());
    putField(PAGE_SIZE, new NumberSchema(NOT_NULL, NOT_NEGATIVE, MAX_100));
    putField(STARTING_TIME_FROM, new StringSchema(NOT_NULL, NOT_BLANK, VALID_ISO_OFFSET_DATE_TIME));
    putField(STARTING_TIME_TO, new StringSchema(NOT_NULL, NOT_BLANK, VALID_ISO_OFFSET_DATE_TIME));
    putField(BUSINESS_KEY, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(EXECUTION_STATUSES, new StringSchema(NOT_NULL, NOT_BLANK, ACCEPTABLE_WORKFLOW_ISNANCE_STATUS));
  }
  
  public ReqWorkflowIstanceSearchSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

}
