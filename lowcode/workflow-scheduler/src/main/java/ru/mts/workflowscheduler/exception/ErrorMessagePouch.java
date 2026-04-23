package ru.mts.workflowscheduler.exception;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorMessagePouch {
  private String systemMessage;
  private Object[] messageAargs;
  private Object[] adviceMessageArgs;
}
