package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqSapStarterDetailsPatch {
  private Optional<Map<String, JsonNode>> serverProps;
  private Optional<Map<String, JsonNode>> destinationProps;
}
