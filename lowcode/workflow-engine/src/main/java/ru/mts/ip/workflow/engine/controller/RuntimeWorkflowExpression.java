package ru.mts.ip.workflow.engine.controller;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.XsdValidation;

@Data
@Accessors(chain = true)
public class RuntimeWorkflowExpression {
  private Map<String, String> secrets;
  private Map<String, JsonNode> args;
  private JsonNode inputValidationSchema;
  private XsdValidation xsdValidation;
  private JsonNode expression;
  private List<String> exposedHttpHeaders;
}
