package ru.mts.ip.workflow.engine.controller.dto;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReqMessage {
  @NotEmpty
  private String businessKey;
  @NotEmpty
  private String messageName;
  @Valid
  private Map<String, JsonNode> variables;
}
