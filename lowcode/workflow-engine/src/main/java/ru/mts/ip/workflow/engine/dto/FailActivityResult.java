package ru.mts.ip.workflow.engine.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;


public record FailActivityResult (Set<String> retryStates, Map<String, JsonNode> variables){
  public FailActivityResult copy(){
    return new FailActivityResult(retryStates, variables);
  }
}
