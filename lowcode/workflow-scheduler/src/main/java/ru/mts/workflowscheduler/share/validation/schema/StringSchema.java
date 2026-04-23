package ru.mts.workflowscheduler.share.validation.schema;

import ru.mts.workflowscheduler.share.validation.Constraint;

import java.util.List;

public class StringSchema extends BaseSchema{

  public StringSchema(Constraint ...constraints) {
    super(List.of(constraints));
    addConstraint(Constraint.TYPE_STRING);
  }

}
