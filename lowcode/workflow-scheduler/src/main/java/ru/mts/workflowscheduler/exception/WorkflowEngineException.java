package ru.mts.workflowscheduler.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class WorkflowEngineException extends RuntimeException {

  private final List<ErrorDescription> descriptions;

  public WorkflowEngineException(List<ErrorDescription> descriptions) {
    this.descriptions = descriptions;
  }

  public WorkflowEngineException(ErrorDescription description) {
    this(List.of(description));
  }

  @Override
  public String getMessage() {
      return descriptions.toString();
  }



}
