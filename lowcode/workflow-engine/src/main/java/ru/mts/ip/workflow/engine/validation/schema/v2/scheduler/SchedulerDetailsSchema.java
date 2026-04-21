package ru.mts.ip.workflow.engine.validation.schema.v2.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;
import java.util.Map;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_SCHEDULER_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.DURATION_NOT_NEGATIVE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_DURATION;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;

public class SchedulerDetailsSchema extends ObjectSchema {

  public final static String TYPE = "type";
  public final static String CRON = "cron";
  public final static String SIMPLE = "simple";
  public final static String SIMPLE_DURATION = "duration";

  public SchedulerDetailsSchema(Constraint...constraint) {
    this(List.of(constraint));
  }

  public SchedulerDetailsSchema(List<Constraint> constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_SCHEDULER_TYPE));
    putField(CRON, new ObjectSchema());
    putField(SIMPLE, new ObjectSchema());
  }

  @Override
  protected BaseSchema afterFilled(JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, TYPE)) {
        var typeJson = json.get(TYPE);
        if(typeJson.isTextual()) {
          String type = typeJson.asText();
          switch(type) {
            case Const.SchedulerType.SIMPLE:
              removeField(CRON);
              return new ObjectSchema(Map.of(
                  SIMPLE, new ObjectSchema(Map.of(SIMPLE_DURATION, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, VALID_DURATION, DURATION_NOT_NEGATIVE)),
                      FILLED, NOT_NULL)
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
