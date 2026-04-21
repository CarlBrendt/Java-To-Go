package ru.mts.ip.workflow.engine.validation.schema.v1;

import static ru.mts.ip.workflow.engine.validation.Constraint.NOT_NULL;
import java.util.List;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.BaseSchema;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;

public class DatabaseCallResultMockSchema extends ObjectSchema{
  
  public final static String RESULT_LIST = "resultList";

  public DatabaseCallResultMockSchema(List<Constraint> constraints) {
    super(constraints);
    putField(RESULT_LIST, new ArraySchema(new BaseSchema(List.of(NOT_NULL)), NOT_NULL));
  }
  
  public DatabaseCallResultMockSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }

}
