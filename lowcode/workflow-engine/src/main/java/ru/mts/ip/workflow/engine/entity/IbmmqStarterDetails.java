package ru.mts.ip.workflow.engine.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.IbmmqConnection;

@Data
@Accessors(chain = true)
public class IbmmqStarterDetails {
  private String queueName;
  private JsonNode payloadValidateSchema;
  private JsonNode outputTemplate;
  private IbmmqConnection connectionDef;
}
