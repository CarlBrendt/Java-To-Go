package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class ComplexResultMockSchema extends ObjectSchema{
  
  public final static String RESULT = "result";

  public ComplexResultMockSchema(List<Constraint> constraints) {
    super(constraints);
    putField(RESULT, new BaseSchema(List.of(NOT_NULL)));
  }
  
  public ComplexResultMockSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

}
