package ru.mts.workflowscheduler.validation.schema.v1;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowscheduler.service.Const;
import ru.mts.workflowscheduler.share.validation.Constraint;
import ru.mts.workflowscheduler.share.validation.schema.BaseSchema;
import ru.mts.workflowscheduler.share.validation.schema.ObjectSchema;
import ru.mts.workflowscheduler.share.validation.schema.StringSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.workflowscheduler.share.validation.Constraint.ACCEPTABLE_SCHEDULER_TYPE;
import static ru.mts.workflowscheduler.share.validation.Constraint.FILLED;
import static ru.mts.workflowscheduler.share.validation.Constraint.NOT_BLANK;
import static ru.mts.workflowscheduler.share.validation.Constraint.NOT_NULL;
import static ru.mts.workflowscheduler.share.validation.Constraint.VALID_DURATION;
import static ru.mts.workflowscheduler.share.validation.Constraint.DURATION_NOT_NEGATIVE;
import static ru.mts.workflowscheduler.share.validation.Constraint.VALID_OFFSET_DATE_TIME;
import static ru.mts.workflowscheduler.share.validation.ValidationHelper.isFiled;
import static ru.mts.workflowscheduler.share.validation.ValidationHelper.isObject;

public class SchedulerStarterSchema extends ObjectSchema {
  public final static String TYPE = "type";
  public final static String START_DATE_TIME = "startDateTime";
  public final static String END_DATE_TIME = "endDateTime";
  public final static String CRON = "cron";
  public final static String SIMPLE = "simple";
  public final static String SIMPLE_DURATION = "duration";

  public SchedulerStarterSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

  public SchedulerStarterSchema(List<Constraint> constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SCHEDULER_TYPE));
    putField(START_DATE_TIME, new StringSchema(VALID_OFFSET_DATE_TIME));
    putField(END_DATE_TIME, new StringSchema(VALID_OFFSET_DATE_TIME));
    putField(CRON, new ObjectSchema());
    putField(SIMPLE, new ObjectSchema());
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFiled(json, TYPE)) {
        var typeJson = json.get(TYPE);
        if(typeJson.isTextual()) {
          String type = typeJson.asText();
          switch(type) {
            case Const.SchedulerType.SIMPLE:
              removeField(CRON);
              return new ObjectSchema(Map.of(
                  SIMPLE, new ObjectSchema(Map.of(SIMPLE_DURATION, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE)), FILLED, NOT_NULL)
              ));
            case Const.SchedulerType.CRON:
              removeField(SIMPLE);
              return new ObjectSchema(Map.of(
                  CRON, new SchedulerCronSchema(FILLED, NOT_NULL)
              ));
          }
        }
      }
    }
    return null;
  }
}
