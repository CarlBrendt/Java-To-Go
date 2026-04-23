package ru.mts.ip.workflow.engine.service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.UUID;

@Data
public class StarterExclusion {
  private UUID starterId;
  private JsonNode details;
}
