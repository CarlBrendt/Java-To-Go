package ru.mts.ip.workflow.engine.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class SapStarterDetails {
  private Map<String, JsonNode> serverProps;
  private Map<String, JsonNode> destinationProps;
}
