package ru.mts.ip.workflow.engine.validation.schema.v2.scheduler;

import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

import java.util.List;

import static ru.mts.ip.workflow.engine.validation.Constraint.CRON_DAY_OF_MONTH;
import static ru.mts.ip.workflow.engine.validation.Constraint.CRON_DAY_OF_WEEK;
import static ru.mts.ip.workflow.engine.validation.Constraint.CRON_HOUR;
import static ru.mts.ip.workflow.engine.validation.Constraint.CRON_MIN;
import static ru.mts.ip.workflow.engine.validation.Constraint.CRON_MONTH;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;


public class SchedulerCronSchema extends ObjectSchema {

  public final static String DAY_OF_THE_WEEK = "dayOfWeek";
  public final static String MONTH = "month";
  public final static String DAY_OF_THE_MONTH = "dayOfMonth";
  public final static String HOUR = "hour";
  public final static String MINUTE = "minute";

  public SchedulerCronSchema(Constraint...constraints) {
    this(List.of(constraints));
  }
  
  public SchedulerCronSchema(List<Constraint> constraints) {
    super(constraints);
    putField(DAY_OF_THE_WEEK, new StringSchema(CRON_DAY_OF_WEEK, FILLED, NOT_NULL, NOT_BLANK));
    putField(MONTH, new StringSchema(CRON_MONTH, FILLED, NOT_NULL, NOT_BLANK));
    putField(DAY_OF_THE_MONTH, new StringSchema(CRON_DAY_OF_MONTH, FILLED, NOT_NULL, NOT_BLANK));
    putField(HOUR, new StringSchema(CRON_HOUR, FILLED, NOT_NULL, NOT_BLANK));
    putField(MINUTE, new StringSchema(CRON_MIN, FILLED, NOT_NULL, NOT_BLANK));
  }
  

}
