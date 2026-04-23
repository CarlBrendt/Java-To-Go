package ru.mts.ip.workflow.engine.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class Variables {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private Map<String, JsonNode> vars = new HashMap<>();
  private JsonNode planeValue;

  public JsonNode asNode() {
    return OBJECT_MAPPER.valueToTree(vars);
  }

  public String asText() {
    return asNode().toString();
  }
  
  public Variables(Map<String, JsonNode> vars) {
    this.vars = vars == null ?  new HashMap<>() : vars;
  }

  public JsonNode remove(String name) {
    return vars.remove(name);
  }
  
  public boolean contains(String varName) {
    return vars.containsKey(varName);
  }

  public Variables exclude(Collection<String> names) {
    var res = new HashMap<>(vars);
    names.forEach(res::remove);
    return new Variables(res);
  }

  public Variables(JsonNode json) {
    this.vars = new HashMap<>();
    if (json.isObject()) {
      var it = json.fields();
      while (it.hasNext()) {
        var field = it.next();
        this.vars.put(field.getKey(), field.getValue());
      }
    }
  }

  public Variables(Variables vars) {
    this(vars.vars);
  }

  public Variables() {
    this(new HashMap<>());
  }

  public <T> Optional<T> find(String name, Class<T> type) {
    JsonNode node = vars.get(name);
    if (node != null) {
      try {
        return Optional.of(OBJECT_MAPPER.treeToValue(node, type));
      } catch (Exception ignore) {
      }
    }
    return Optional.empty();
  }

  public Optional<JsonNode> findDeep(String fieldName){
   var res = find(fieldName, JsonNode.class);
   if (res.isPresent()) {
     return res;
   }
    for (JsonNode node : vars.values()) {
      JsonNode found = findDeep(node,fieldName);
      if (found != null) {
        return Optional.of(found);
      }
    }
    return Optional.empty();
  }

  private JsonNode findDeep(JsonNode node, String fieldName) {
    if (node.has(fieldName)) {
      return node.get(fieldName);
    }

    for (JsonNode child : node) {
      JsonNode result = findDeep(child, fieldName);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  public Optional<String> findString(String name) {
    return find(name, String.class);
  }

  public Optional<Integer> findInteger(String name) {
    return find(name, Integer.class);
  }

  public Variables put(String string, JsonNode readTree) {
    vars.put(string, readTree);
    return this;
  }

  public Variables put(String string, Object obj) {
    vars.put(string, OBJECT_MAPPER.valueToTree(obj));
    return this;
  }

  public void putAll(Variables another) {
    putAll(another.vars);
  }

  public static Variables merge(List<Variables> list) {
    Variables res = new Variables();
    list.forEach(res::putAll);
    return res;
  }

  public EvaluationContext asStandardEvaluationContext() {
    EvaluationContext res = new StandardEvaluationContext();
    vars.forEach((k, v) -> {
      if (v.isTextual()) {
        res.setVariable(k, v.asText());
      } else if (v.isNumber()) {
        res.setVariable(k, v.asInt());
      } else if (v.isBoolean()) {
        res.setVariable(k, v.asBoolean());
      } else {
        res.setVariable(k, v);
      }
    });
    return res;
  }

  public Variables copy() {
    var res = new Variables();
    res.putAll(this);
    return res;
  }

  public void putAll(Map<String, JsonNode> args) {
    vars.putAll(args);
  }

}
