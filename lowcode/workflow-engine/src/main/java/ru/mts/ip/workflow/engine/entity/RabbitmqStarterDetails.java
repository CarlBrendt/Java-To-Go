package ru.mts.ip.workflow.engine.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.RabbitmqConnection;

@Data
@Accessors(chain = true)
public class RabbitmqStarterDetails {
  private JsonNode payloadValidateSchema;
  private JsonNode headersValidateSchema;
  private JsonNode outputTemplate;
  private String queue;
  private RabbitmqConnection connectionDef;
}
