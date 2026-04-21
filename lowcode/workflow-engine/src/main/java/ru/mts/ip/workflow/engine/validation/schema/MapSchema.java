package ru.mts.ip.workflow.engine.validation.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;

public class MapSchema extends BaseSchema{

  public MapSchema(BaseSchema valueField, Constraint ...constraints) {
    this(valueField, List.of(constraints));
  }
  
  public MapSchema(BaseSchema valueField, List<Constraint> constraints) {
    super(constraints);
    addConstraint(Constraint.TYPE_OBJECT);
    Objects.requireNonNull(valueField);
    this.valueField = valueField;
  }
  
  
  private final BaseSchema valueField;
  private final Map<String, BaseSchema> nested = new HashMap<>();
  
  @Override
  protected Map<String, BaseSchema> getNested() {
    return nested;
  }

  @Override
  public BaseSchema copy() {
    return new MapSchema(valueField, copyConstraints());
  }

  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    if(!containErrors()) {
      if(json != null) {
        if(json.getNodeType() == JsonNodeType.OBJECT) {
          for(var entry : json.properties()) {
            var k = entry.getKey();
            var v = entry.getValue();
            var field = valueField.copy();
            field.initPath("%s.%s".formatted(getPath(), k));
            nested.put(k, field);
            field.validate(ctx, v);
          }
        }
      }
    }
  }

}
