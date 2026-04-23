package ru.mts.workflowscheduler.share.validation.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import ru.mts.workflowscheduler.share.validation.Constraint;
import ru.mts.workflowscheduler.share.validation.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ArraySchema extends BaseSchema{

  public ArraySchema(BaseSchema valueField, Constraint...constraints) {
    this(valueField, List.of(constraints));
  }

  public ArraySchema(BaseSchema valueField, List<Constraint> constraints) {
    super(constraints);
    addConstraint(Constraint.TYPE_ARRAY);
    Objects.requireNonNull(valueField);
    this.valueField = valueField;
  }

  private BaseSchema valueField;
  private Map<String, BaseSchema> nested = new HashMap<>();

  @Override
  protected Map<String, BaseSchema> getNested() {
    return nested;
  }

  @Override
  public BaseSchema copy() {
    var res =  new ArraySchema(valueField, copyConstraints());
    return res;
  }

  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    if(!containErrors()) {
      if(json != null) {
        if(json.getNodeType() == JsonNodeType.ARRAY) {
          for(int i = 0; i < json.size(); i++) {
            var k = "[%d]".formatted(i);
            var v = json.get(i);
            var arrayPath = getPath() == null ? k : "%s[%d]".formatted(getPath(), i);
            var field = valueField.copy();
            field.initPath(arrayPath);
            nested.put(k, field);
            field.validate(ctx, v);
          }
        }
      }
    }
  }

}
