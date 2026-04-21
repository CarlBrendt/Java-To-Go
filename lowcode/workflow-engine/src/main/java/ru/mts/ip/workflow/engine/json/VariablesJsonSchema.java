package ru.mts.ip.workflow.engine.json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariablesJsonSchema {

private String title;
  
  private JsonNode type;
  
  private String stringFormat;
  
  @Valid
  private Map<@NotBlank String, @NotNull @Valid Property> properties = Map.of();
  
  @Valid
  private Map<@NotBlank String, @NotNull @Valid Property> definitions = Map.of();

  private List<@NotBlank String> required;

  @JsonIgnore
  private List<String> enums;
  
  @JsonIgnore
  private JsonNode items;

  @JsonIgnore
  private String ref;

  @JsonIgnore
  private Integer minLength;

  @JsonIgnore
  private JsonNode src;

  public Property asProperty() {
    return new Property().setItems(items).setProperties(properties).setRef(ref).setType(type);
  }
  
  public VariablesJsonSchema copy() {
    return new VariablesJsonSchema()
        .setDefinitions(definitions == null ? null : definitions.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().copy())))
        .setItems(items == null ? null :  items.deepCopy())
        .setProperties(properties == null ? null : properties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().copy())))
        .setRef(ref)
        .setRequired(required)
        .setSrc(src)
        .setTitle(title)
        .setType(type);
    
  }

  @Data
  @Valid
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Property {
    
    private JsonNode type;
    
    private String stringFormat;
    
    private String description;

    @JsonProperty("$ref")
    private String ref;
    
    @Valid
    private JsonNode items;
    
    private List<@NotBlank String> required;

    @JsonProperty("enum")
    private List<String> enums;
    
    private Integer minLength;

    @Valid
    private Map<@NotBlank String, @Valid Property> properties = Map.of();

    public VariablesJsonSchema asVariablesJsonSchema() {
      return new VariablesJsonSchema()
          .setStringFormat(stringFormat)
          .setType(type)
          .setItems(items)
          .setRef(ref)
          .setMinLength(minLength)
          .setEnums(enums)
          .setRequired(required)
          .setProperties(properties);
    }

    public Property copy() {
      return new Property().setDescription(description)
          .setItems(items == null ? null :  items.deepCopy())
          .setProperties(properties == null ? null
              : properties.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().copy()))
          ).setType(type)
          ;
    }
  }
  
}
