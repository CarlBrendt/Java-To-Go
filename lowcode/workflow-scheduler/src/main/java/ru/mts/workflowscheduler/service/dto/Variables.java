package ru.mts.workflowscheduler.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public class Variables {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private Map<String, JsonNode> vars = new HashMap<>();

  public Variables(Map<String, JsonNode> vars) {
    this.vars = vars;
  }

  public JsonNode asNode() {
    return OBJECT_MAPPER.valueToTree(vars);
  }

  public String asText() {
    return asNode().toString();
  }

  public boolean contains(String varName) {
    return vars.containsKey(varName);
  }

  public Variables(JsonNode json) {
    this.vars = new HashMap<>();
    if(json != null) {
      if (json.isObject()) {
        var it = json.fields();
        while (it.hasNext()) {
          var field = it.next();
          this.vars.put(field.getKey(), field.getValue());
        }
      }
    }
  }

  public Variables(Variables vars) {
    this(vars.vars);
  }

  public Variables() {
    this(new HashMap<>());
  }


  public Optional<JsonNode> find(String name) {
    return Optional.ofNullable(vars.get(name));
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
    vars.putAll(another.vars);
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

}
