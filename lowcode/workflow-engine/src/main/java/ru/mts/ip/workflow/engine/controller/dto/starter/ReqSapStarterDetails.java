package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class ReqSapStarterDetails {
  private Map<String, JsonNode> serverProps;
  private Map<String, JsonNode> destinationProps;
}
