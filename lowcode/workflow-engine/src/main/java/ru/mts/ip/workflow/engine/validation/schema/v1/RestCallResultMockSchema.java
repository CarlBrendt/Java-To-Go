package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_HTTP_STATUS_CODE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_EMPTY_ARRAY;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;;

public class RestCallResultMockSchema extends ObjectSchema {
  
  public final static String CONTENT_TYPE = "contentType";
  public final static String BODY_EXAMPLE = "bodyExample";
  public final static String RESP_CODE = "respCode";
  public final static String HEADERS = "headers";
  
  
  public RestCallResultMockSchema(Constraint ...constraints) {
    super(constraints);
    putField(CONTENT_TYPE, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(BODY_EXAMPLE, new BaseSchema());
    putField(RESP_CODE, new NumberSchema(FILLED, NOT_NULL, ACCEPTABLE_HTTP_STATUS_CODE));
    putField(HEADERS, new MapSchema(new ArraySchema(new StringSchema(NOT_NULL), NOT_EMPTY_ARRAY)));
  }

}
