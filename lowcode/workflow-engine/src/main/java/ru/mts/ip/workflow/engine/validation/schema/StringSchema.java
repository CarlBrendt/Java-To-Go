package ru.mts.ip.workflow.engine.validation.schema;

import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;

public class StringSchema extends BaseSchema{

  public StringSchema(Constraint ...constraints) {
    super(List.of(constraints));
    addConstraint(Constraint.TYPE_STRING);
  }

}