package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
public class ResStopWorkflow {
  private final int total;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final List<ResStopWorkflowDetail> details;

  public ResStopWorkflow(List<ResStopWorkflowDetail> details) {
    this(details.size(), details);
  }

  public ResStopWorkflow(int total) {
    this(total, null);
  }

  private ResStopWorkflow(int total, List<ResStopWorkflowDetail> details) {
    this.total = total;
    this.details = details;
  }

  public record ResStopWorkflowDetail(String runId, String businessKey) {
  }
}
