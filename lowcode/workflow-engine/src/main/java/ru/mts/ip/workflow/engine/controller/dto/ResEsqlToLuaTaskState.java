package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowDefinitionErrorDescription.ResErrorDescription;

@Data
public class ResEsqlToLuaTaskState {
  private List<ResErrorDescription> errors;
  private String taskId;
  private JsonNode workflowDefinition;
  private String status;
}
