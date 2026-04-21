package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.ACCEPTABLE_WORKFLOW_TYPE;
import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_BLANK;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import static ru.mts.ip.workflow.engine.validation.Constraint.VALID_ACTIVITY;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isFilled;
import static ru.mts.ip.workflow.engine.validation.ValidationHelper.isObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.MapSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class PlantTailSchema extends ObjectSchema{

  public final static String TYPE = "type";
  public final static String NAME = "name";
  public static final String DESCRIPTION = "description";
  public final static String TENANT_ID = "tenantId";
  public final static String ACTIVITIES = "activities";
  public final static String DETAILS = "details";
  
  public PlantTailSchema(Constraint... constraints) {
    super(constraints);
    putField(TYPE, new StringSchema(FILLED, NOT_NULL, NOT_BLANK, ACCEPTABLE_WORKFLOW_TYPE));
    putField(NAME, new StringSchema(FILLED, NOT_NULL, NOT_BLANK));
    putField(DESCRIPTION, new StringSchema());
    putField(TENANT_ID, new StringSchema(NOT_NULL, NOT_BLANK));
    putField(ACTIVITIES, new MapSchema(new ObjectSchema(VALID_ACTIVITY), FILLED, NOT_NULL));
    putField(DETAILS, new ObjectSchema());
  }
  
  @Override
  protected void enrichContext(Context ctx, JsonNode json) {
    if(isObject(json)) {
      if(isFilled(json, DETAILS)) {
        var details = json.get(DETAILS);
        if(details != null) {
          if(isObject(details)) {
            Map<String, List<JsonNode>> allActivities = new HashMap<>();
            details.fieldNames().forEachRemaining(name -> {
              var activityJson = details.get(name);
              var listsOfActivities = allActivities.getOrDefault(name, new ArrayList<>());
              listsOfActivities.add(activityJson);
              allActivities.put(name, listsOfActivities);
            });
            ctx.setActivities(allActivities);
          }
        }
      }
    }
  }
  
}
