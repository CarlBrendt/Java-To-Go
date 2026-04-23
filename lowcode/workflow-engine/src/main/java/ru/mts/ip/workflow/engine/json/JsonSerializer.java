package ru.mts.ip.workflow.engine.json;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import ru.mts.ip.workflow.engine.controller.dto.ReqStarter;
import ru.mts.ip.workflow.engine.dto.DetailedWorkflowDefinition;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.service.Variables;

public interface JsonSerializer {
  JsonNode toJson(Object object);
  DetailedWorkflowDefinition toDetailedWorkflowDefinition(WorkflowDefinition def);
  List<ReqStarter> toListOfStarters(JsonNode json);
  <T> T treeToValue(JsonNode schema, Class<T> class1);
  Variables geneateVariables(JsonNode json);
  ArrayNode createArrayNode();
}
