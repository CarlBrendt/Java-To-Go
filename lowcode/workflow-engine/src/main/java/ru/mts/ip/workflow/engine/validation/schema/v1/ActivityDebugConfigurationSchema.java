package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.FILLED;
import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ConstraintViolation;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class ActivityDebugConfigurationSchema extends ObjectSchema {
  
  public final static String REST_CALL_RESULT_MOCK = "restCallResultMock";
  public final static String COMPLEX_RESULT_MOCK = "complexResultMock";
  public final static String AWAIT_FOR_MESSAGE_RESULT_MOCK = "awaitForMessageResultMock";
  public final static String DB_CALL_RESULT_MOCK = "dbCallResultMock";
  public final static String ACTIVITY_ID = "activityId";
  
  public ActivityDebugConfigurationSchema(List<Constraint> constraints) {
    super(constraints);
    putField(REST_CALL_RESULT_MOCK, new RestCallResultMockSchema());
    putField(COMPLEX_RESULT_MOCK, new ComplexResultMockSchema());
    putField(AWAIT_FOR_MESSAGE_RESULT_MOCK, new AwaitForMessageResultMockSchema());
    putField(DB_CALL_RESULT_MOCK, new DatabaseCallResultMockSchema());
    putField(ACTIVITY_ID, new StringSchema(FILLED, NOT_NULL));
    
  }
  
  public ActivityDebugConfigurationSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }
  
  @Override
  public BaseSchema copy() {
    var res = new ActivityDebugConfigurationSchema(copyConstraints());
    getNested().forEach((k, v) -> res.getNested().put(k, v.copy()));
    return res;
  } 
  
  public List<ConstraintViolation> getViolations(){
    List<ConstraintViolation> res = new ArrayList<>(violations);
    getNested().values().forEach(f -> res.addAll(f.getViolations()));
    res.forEach(v -> v.setActivityId(activityId));
    return res;
  }
  
  protected String activityId;
  
  protected BaseSchema afterFilled(JsonNode json) {
    if(json != null) {
      if(json.isObject()) {
        activityId = Optional.ofNullable(json.get(ACTIVITY_ID)).filter(JsonNode::isTextual).map(JsonNode::asText).orElse(null);
      }
    }
    return null;
  }
  
  
}
