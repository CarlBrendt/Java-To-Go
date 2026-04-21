package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.UUID;

@Data
public class ReqStarterExclusion {
  private UUID starterId;
  private JsonNode details;
}
