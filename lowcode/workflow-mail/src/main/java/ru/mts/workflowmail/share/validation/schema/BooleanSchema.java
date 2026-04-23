package ru.mts.workflowmail.share.validation.schema;


import ru.mts.workflowmail.share.validation.Constraint;

import java.util.List;

public class BooleanSchema extends BaseSchema{

  public BooleanSchema(Constraint...constraints) {
    super(List.of(constraints));
    addConstraint(Constraint.TYPE_BOOLEAN);
  }

}
