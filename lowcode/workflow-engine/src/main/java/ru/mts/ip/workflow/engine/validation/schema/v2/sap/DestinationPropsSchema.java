package ru.mts.ip.workflow.engine.validation.schema.v2.sap;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.NumberSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;

public class DestinationPropsSchema extends ObjectSchema {
  
  private static final String DESTINATION_POOL_CAPACITY = "jco.destination.pool_capacity";
  private static final String DESTINATION_PEAK_LIMIT = "jco.destination.peak_limit";
  private static final String CLIENT_ASHOST = "jco.client.ashost";
  private static final String CLIENT_USER = "jco.client.user";
  private static final String CLIENT_PASSWD = "jco.client.passwd";
  private static final String CLIENT_LANG = "jco.client.lang";
  private static final String CLIENT_CLIENT = "jco.client.client";
  private static final String CLIENT_SYSNR = "jco.client.sysnr";
  
  public DestinationPropsSchema(Constraint... constraints) {
    super(constraints);
    putField(DESTINATION_POOL_CAPACITY, new NumberSchema(NOT_NULL));
    putField(DESTINATION_PEAK_LIMIT, new NumberSchema(NOT_NULL));
    putField(CLIENT_ASHOST, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CLIENT_USER, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CLIENT_PASSWD, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(CLIENT_LANG, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(CLIENT_CLIENT, new NumberSchema(FILLED, NOT_NULL));
    putField(CLIENT_SYSNR, new NumberSchema(FILLED, NOT_NULL));
  }

  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
  }
  
  

}
