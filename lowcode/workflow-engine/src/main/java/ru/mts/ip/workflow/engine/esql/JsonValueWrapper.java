package ru.mts.ip.workflow.engine.esql;

import java.util.Objects;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

public class JsonValueWrapper {
  
  private static final ObjectMapper OM = new ObjectMapper();
  @Getter
  private final String path;
  
  public JsonValueWrapper(JsonNode node) {
    this(node, "$");
  }

  public JsonValueWrapper() {
    this(OM.createObjectNode(), "$");
  }

  JsonValueWrapper(JsonNode node, String path) {
    this.node = node;
    this.path = path;
  }

  private final JsonNode node;

  public JsonValueWrapper get(String key) {
    JsonNode res = null;
    if(node != null && !node.isNull() && node.isObject()) {
      res = node.get(key);
    }
    return new JsonValueWrapper(res, "%s.%s".formatted(path, key));
  }

  public Optional<String> asString() {
    if(node != null && node.isTextual()) {
      return Optional.of(node.textValue());
    }
    return Optional.empty();
  }
  
  public JsonValueWrapper get(int index) {
    JsonNode res = null;
    if(node != null && !node.isNull() && node.isArray()) {
      res = node.get(index);
    }
    return new JsonValueWrapper(res, "%s.[%d]".formatted(path, index));
  }
  
  public JsonValueWrapper findByFieldValue(String fieldName, String value) {
    if(isArray()) {
      var arr = (ArrayNode) node;
      for(int i = 0; i < arr.size(); i++) {
        var item = arr.get(i);
        if(item != null && item.isObject()) {
          var itemFieldValue = item.get(fieldName);
          if(itemFieldValue.isTextual() && Objects.equals(itemFieldValue.asText(), value)) {
            return new JsonValueWrapper(item, "%s.[%d]".formatted(path, i));
          }
        }
      }
    }
    return new JsonValueWrapper(null, "%s.[%s='%s']".formatted(path, fieldName, value));
  }
  
  public JsonValueWrapper add(JsonNode value) {
    if(isArray()) {
      ((ArrayNode) node).add(value);
    }
    return this;
  }  
  
  public JsonValueWrapper put(String key, JsonNode value) {
    if(isObject()) {
      ((ObjectNode) node).set(key, value);
    }
    return this;
  }  
  
  public boolean isExists() {
    return node != null;
  }
  
  public JsonValueWrapper put(String key, Object value) {
    return put(key, OM.valueToTree(value));
  }
  
  public JsonValueWrapper add(Object value) {
    return add(OM.valueToTree(value));
  }
  
  private boolean isObject() {
    return node != null && node.isObject();
  }

  private boolean isArray() {
    return node != null && node.isArray();
  }
  
  public static JsonValueWrapper fromNode(JsonNode node) {
    return new JsonValueWrapper(node);
  }

  public JsonNode getNode() {
    return node;
  }
  
  
}
