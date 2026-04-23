package ru.mts.workflowmail.exception;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorContext {

  private Object rejectedValue;
  private String systemMessage;

  public ErrorContext(Object rejectedValue) {
    this.rejectedValue = rejectedValue;
  }

  public ErrorContext() {

  }

}
