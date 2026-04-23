package ru.mts.workflowmail.share.validation.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import ru.mts.workflowmail.share.validation.Constraint;
import ru.mts.workflowmail.share.validation.Context;
import ru.mts.workflowmail.share.validation.Errors2;
import ru.mts.workflowmail.share.validation.ValidateDecision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ObjectSchema extends BaseSchema{

  public ObjectSchema(Constraint...constraints) {
    this(List.of(constraints));
  }

  public ObjectSchema(List<Constraint> constraints) {
    super(constraints);
    addConstraint(Constraint.TYPE_OBJECT);
  }

  public ObjectSchema() {
    this(new Constraint[]{});
  }

  public ObjectSchema(Map<String, BaseSchema> fields) {
    this(new Constraint[]{});
    fields.forEach(this::putField);
  }

  public ObjectSchema(Map<String, BaseSchema> fields, Constraint ...constraints) {
    this(constraints);
    fields.forEach(this::putField);
  }

  private Map<String, BaseSchema> nested = new HashMap<>();
  protected BaseSchema afterFiled;

  @Override
  protected Map<String, BaseSchema> getNested() {
    var res =  nested;
    if(afterFiled != null) {
      res = new HashMap<>(nested);
      res.putAll(afterFiled.getNested());
    }
    return res;
  }

  protected void putField(String name, BaseSchema value) {
    if(name.contains(".")) {
      name = "['%s']".formatted(name);
    }
    nested.put(name, value);
  }

  protected void removeField(String name) {
    nested.remove(name);
  }

  protected void removeFields(String ...names) {
    Stream.of(names).forEach(nested::remove);
  }

  protected BaseSchema afterFilled(JsonNode json) {
    return null;
  }

  @Override
  public void validate(Context ctx, JsonNode json) {
    super.validate(ctx, json);
    if(!containErrors()) {
      if(json != null) {
        if(json.getNodeType() == JsonNodeType.OBJECT) {

          nested.forEach((k,v) -> {
            var valJson = json.get(k);
            var fieldPath = getPath() == null ? k : "%s.%s".formatted(getPath(), k);
            v.initPath(fieldPath);
            v.validate(ctx, valJson);
          });

          afterFiled = afterFilled(json);
          if(afterFiled != null) {
            afterFiled.initPath(getPath());
            afterFiled.validate(ctx, json);
          }

          for(var entry : json.properties()) {
            var k = entry.getKey();
            var nested = getNested();
            if(!nested.isEmpty()) {
              var field = nested.get(k);
              k = k.contains(".") ? "['%s']".formatted(k) : k;
              var fieldPath = getPath() == null ? k : "%s.%s".formatted(getPath(), k);
              if(field == null) {
                ValidateDecision des = new ValidateDecision(Errors2.UNKNOWN_FIELD);
                violations.add(new ConstraintViolation().setError(des.getError()).setPath(fieldPath));
              }
            }
          }

        }
      }
    }
  }

  @Override
  public BaseSchema copy() {
    var res =  new ObjectSchema(copyConstraints());
    nested.forEach((k, v) -> {
      res.nested.put(k, v.copy());
    });
    if(afterFiled != null) {
      res.afterFiled = afterFiled.copy();
    }
    return res;
  }



}
