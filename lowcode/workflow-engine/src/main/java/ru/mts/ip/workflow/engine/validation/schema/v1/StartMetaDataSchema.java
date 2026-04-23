package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class StartMetaDataSchema extends ObjectSchema {

  public static final String IMS = "ims";
  public static final String POSITION = "position";
  public static final String IMS_ACTIVITY_PRODUCT = "imsActivityProduct";
  public static final String IS_DEVELOPER_MODE = "isDeveloperMode";
  public static final String IS_DEVELOPER_MODE_VERIFY = "isDeveloperModeVerify";
  public static final String IS_DEVELOPER_MODE_ERROR = "isDeveloperModeError";
  public static final String DEVELOPER_MODE_ERROR = "developerModeErrors";
  public static final String DEBUG_V2_IS_LAST = "debugV2IsLast";

  public StartMetaDataSchema(Constraint... constraints) {
    super(constraints);
    putField(IMS, new StringSchema());
    putField(POSITION, new BaseSchema());
    putField(IMS_ACTIVITY_PRODUCT, new BaseSchema());
    putField(IS_DEVELOPER_MODE, new BaseSchema());
    putField(IS_DEVELOPER_MODE_VERIFY, new BaseSchema());
    putField(IS_DEVELOPER_MODE_ERROR, new BaseSchema());
    putField(DEVELOPER_MODE_ERROR, new BaseSchema());
    putField(DEBUG_V2_IS_LAST, new BaseSchema());
  }
}
