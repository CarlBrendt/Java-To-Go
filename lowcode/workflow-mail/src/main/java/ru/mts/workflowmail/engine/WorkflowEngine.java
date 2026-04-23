package ru.mts.workflowmail.engine;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.workflowmail.service.dto.Ref;

import java.util.UUID;

public interface WorkflowEngine {
  public void startFlow(Ref definitionRef, String businessKey, UUID workerId);
  public void startFlow(Ref definitionRef, String businessKey, JsonNode vars, UUID workerId);
}
