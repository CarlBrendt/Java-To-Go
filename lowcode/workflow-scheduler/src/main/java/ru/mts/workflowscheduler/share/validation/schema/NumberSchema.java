package ru.mts.workflowscheduler.share.validation.schema;

import ru.mts.workflowscheduler.share.validation.Constraint;

import java.util.List;

public class NumberSchema extends BaseSchema{

  public NumberSchema(Constraint ...constraints) {
    super(List.of(constraints));
    addConstraint(Constraint.TYPE_NUMBER);
  }

}
