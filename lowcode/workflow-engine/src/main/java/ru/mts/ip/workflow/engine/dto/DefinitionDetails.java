package ru.mts.ip.workflow.engine.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefinitionDetails {
  private JsonNode inputValidateSchema;
  private JsonNode outputValidateSchema;
  private XsdValidation xsdValidation;
  private YamlValidation yamlValidation;
  private WorkflowAccessList initialAppendAccessConfigCommand;
  private List<Starter> starters;
  private Map<String, String> secrets;
  private List<String> exposedHttpHeaders;
}
