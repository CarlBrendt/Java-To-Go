package ru.mts.ip.workflow.engine.controller.dto.starter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ResStarterExclusion {
  private UUID starterId;
  private OffsetDateTime createTime;
  private JsonNode details;
}
