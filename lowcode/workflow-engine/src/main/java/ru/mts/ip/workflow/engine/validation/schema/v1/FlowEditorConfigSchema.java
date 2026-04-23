package ru.mts.ip.workflow.engine.validation.schema.v1;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class FlowEditorConfigSchema extends ObjectSchema {
  public static final String START_META_DATA = "startMetadata";
  public static final String ACTIVITY_META_DATA = "activityMetadata";
  public static final String IMS_SCHEMA_PRODUCT = "imsSchemaProduct";
  public static final String HORIZONTAL_LAYOUT = "horizontalLayout";

  public FlowEditorConfigSchema(Constraint... constraints) {
    super(constraints);
    putField(START_META_DATA, new StartMetaDataSchema());
    putField(ACTIVITY_META_DATA, new BaseSchema());
    putField(IMS_SCHEMA_PRODUCT, new BaseSchema());
    putField(HORIZONTAL_LAYOUT, new BaseSchema());
  }
}
