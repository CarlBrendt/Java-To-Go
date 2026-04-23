package ru.mts.ip.workflow.engine.exception;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorMessageArgs {
  private Object[] messageArgs;
  private Object[] adviceMessageArgs;
  
  public ErrorMessageArgs and(Object ...args) {
    if(messageArgs == null) {
      messageArgs = args;
    } else if(adviceMessageArgs == null) {
      adviceMessageArgs = args;
    } else {
      throw new IllegalStateException("wrong usage");
    }
    return this;
  }
  
}
