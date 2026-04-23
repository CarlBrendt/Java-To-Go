package ru.mts.ip.workflow.engine.validation.schema;

import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;

public class NumberSchema extends BaseSchema{

  public NumberSchema(Constraint ...constraints) {
    super(List.of(constraints));
    addConstraint(Constraint.TYPE_NUMBER);
  }

}
