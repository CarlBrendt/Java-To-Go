package ru.mts.workflowmail.validation.schema.v1;





import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.schema.ArraySchema;
import ru.mts.workflowmail.share.validation.schema.NumberSchema;
import ru.mts.workflowmail.share.validation.schema.ObjectSchema;
import ru.mts.workflowmail.share.validation.schema.StringSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.workflowmail.share.validation.Constraint.ACCEPTABLE_SORTING_DIRECTION;
import static ru.mts.workflowmail.share.validation.Constraint.ACCEPTABLE_STARTER_SORTING_FIELD;
import static ru.mts.workflowmail.share.validation.Constraint.ACCEPTABLE_STARTER_STATUSES;
import static ru.mts.workflowmail.share.validation.Constraint.FILLED;
import static ru.mts.workflowmail.share.validation.Constraint.MAX_100;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_BLANK;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_NEGATIVE;
import static ru.mts.workflowmail.share.validation.Constraint.NOT_NULL;
import static ru.mts.workflowmail.share.validation.Constraint.VALID_UUID;



public class StarterSearchingSchema extends ObjectSchema {

  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String NAME = "name";
  public static final String TENANT_ID = "tenantId";
  public static final String WORKFLOW_DEFINITION_TO_START_IDS = "workflowDefinitionToStartIds";
  public static final String DESIRED_STATUSES = "desiredStatuses";
  public static final String ACTUAL_STATUSES = "actualStatuses";
  public static final String SORTING = "sorting";
  public static final String SORTING_NAME = "name";
  public static final String SORTING_DIRECTION = "direction";

  public StarterSearchingSchema(Constraint...constraints) {
    super(List.of(constraints));
    putField(OFFSET, new NumberSchema(NOT_NULL, NOT_NEGATIVE));
    putField(LIMIT, new NumberSchema(NOT_NULL, NOT_NEGATIVE, MAX_100));
    putField(NAME, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(TENANT_ID, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(WORKFLOW_DEFINITION_TO_START_IDS, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK, VALID_UUID), NOT_NULL));
    putField(SORTING, new ArraySchema(new ObjectSchema(Map.of(
      SORTING_NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_STARTER_SORTING_FIELD),
      SORTING_DIRECTION, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SORTING_DIRECTION))
    )));
    putField(DESIRED_STATUSES, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK, ACCEPTABLE_STARTER_STATUSES)));
    putField(ACTUAL_STATUSES, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK, ACCEPTABLE_STARTER_STATUSES)));
  }

}
