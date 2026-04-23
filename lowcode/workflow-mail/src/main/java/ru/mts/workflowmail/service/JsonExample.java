package ru.mts.workflowmail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonExample {

  private Map<String, VariablesJsonSchema> definitions;
  private final ObjectMapper om = new ObjectMapper();
  private JsonNode generated;

  private VariablesJsonSchema resolveDescription(VariablesJsonSchema desc) {
    var ref = desc.getRef();
    if (ref != null) {
      return definitions.get(ref.replaceAll("#/definitions/", ""));
    } else {
      return desc;
    }

  }

  public JsonExample(VariablesJsonSchema schema) {
    generated = generate(schema);
  }

  public Variables asVariables() {
    return Optional.ofNullable(generated).map(Variables::new).orElse(new Variables());
  }

  public JsonNode asNode() {
    return generated;
  }


  private JsonNode generate(VariablesJsonSchema desc) {
    desc = resolveDescription(desc);
    if (definitions == null) {
      Optional.ofNullable(desc.getDefinitions()).ifPresent(def -> {
        this.definitions = def.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().asVariablesJsonSchema()));
      });
    }
    var type = Optional.ofNullable(desc.getType()).flatMap(json -> {
      if(json.isTextual()) {
        return Optional.of(json.asText());
      } else if (json.isArray() && json.size() > 0){
        var first = json.get(0);
        if(first.isTextual()) {
          return Optional.of(first.asText());
        }
      }
      return Optional.empty();
    }).orElse("null");
    switch (type) {
      case "number":
        return new DoubleNode(0);
      case "boolean":
        return BooleanNode.FALSE;
      case "integer":
        return new IntNode(0);
      case "string":
        var format = desc.getStringFormat();
        var val = Optional.ofNullable(desc.getEnums()).flatMap(l -> l.stream().findFirst())
          .or(() -> Optional.ofNullable(format))
          .map(stringFormatExamples::get)
          .orElse("text");
        return new TextNode(val);
      case "object":
        var objectNode = om.createObjectNode();
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        desc.getProperties().entrySet().stream().forEach(e -> fields.put(e.getKey(),
            generate(e.getValue().asVariablesJsonSchema())));
        objectNode.setAll(fields);
        return objectNode;
      case "array":
        var arrayNode = om.createArrayNode();
        List<JsonNode> elements = new ArrayList<>();
        desc.getItems().stream().findFirst().map(p -> p.asVariablesJsonSchema())
          .map(this::generate).map(elements::add);
        arrayNode.addAll(elements);
        return arrayNode;
      default:
        return NullNode.instance;
    }
  }

  private final Map<String, String> stringFormatExamples = Map.of("xml", "<root><field1>value</field1></root>", "json", "{\"field1\": \"value\"}");

}
