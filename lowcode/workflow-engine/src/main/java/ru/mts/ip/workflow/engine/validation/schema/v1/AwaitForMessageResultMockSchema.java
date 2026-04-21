package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class AwaitForMessageResultMockSchema extends ObjectSchema{
  
  public final static String MESSAGE = "message";

  public AwaitForMessageResultMockSchema(List<Constraint> constraints) {
    super(constraints);
    putField(MESSAGE, new BaseSchema(List.of(NOT_NULL)));
  }
  
  public AwaitForMessageResultMockSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

}
