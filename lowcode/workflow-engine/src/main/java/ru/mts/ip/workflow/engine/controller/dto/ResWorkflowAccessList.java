package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResWorkflowAccessList(List<ResAccessEntry> accessList) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ResAccessEntry(String clientId) {
  }
}
