package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_DEFINITION_STATUS;
import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_VERSION;
import static ru.mts.ip.workflow.engine.validation.Constraint.MAX_100;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_UUID;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class DefinitionSearchingSchema extends ObjectSchema {
  
  public static final String DESCRIPTION = "description";
  public static final String PRODUCTS = "products";
  public static final String OWNER_LOGIN = "ownerLogin";
  public static final String STATUSES = "statuses";
  public static final String NAME = "name";
  public static final String ID = "id";
  public static final String VERSION = "version";
  public static final String OFFSET = "offset";
  public static final String LIMIT = "limit";
  
  public DefinitionSearchingSchema(Constraint ...constraints) {
    super(constraints);
    putField(DESCRIPTION, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(VERSION, new StringSchema(NOT_NULL, NOT_BLANK, ACCEPTABLE_VERSION));
    putField(PRODUCTS, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK), NOT_NULL));
    putField(OWNER_LOGIN, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(STATUSES, new ArraySchema(new StringSchema(NOT_NULL, NOT_BLANK, ACCEPTABLE_DEFINITION_STATUS)));
    putField(NAME, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(OFFSET, new NumberSchema(NOT_NULL, NOT_NEGATIVE));
    putField(LIMIT, new NumberSchema(NOT_NULL, NOT_NEGATIVE, MAX_100));
    putField(ID, new StringSchema(NOT_NULL, VALID_UUID));
  }

}
