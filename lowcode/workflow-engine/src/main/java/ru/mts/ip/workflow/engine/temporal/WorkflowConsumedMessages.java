package ru.mts.ip.workflow.engine.temporal;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowConsumedMessages {
  private Map<String, List<JsonNode>> messages;
}
