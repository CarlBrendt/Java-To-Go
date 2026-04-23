package ru.mts.workflowscheduler.share.validation.schema;

import lombok.experimental.UtilityClass;
import ru.mts.workflowscheduler.share.validation.Constraint;

import java.util.List;

@UtilityClass
public class InstantiationHelper {


  public BaseSchema string(Constraint ...constraints) {
    return new StringSchema(constraints);
  }

  public MapSchema mapStringTo(BaseSchema valueType, Constraint ...constraints) {
    return new MapSchema(valueType, constraints);
  }

  public BaseSchema field(Constraint ...constraints) {
    return new BaseSchema(List.of(constraints));
  }

  public BaseSchema arrayOf(BaseSchema valueType, Constraint ...constraints) {
    return new ArraySchema(valueType, constraints);
  }



}
