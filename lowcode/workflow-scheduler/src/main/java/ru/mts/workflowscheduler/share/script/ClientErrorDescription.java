package ru.mts.workflowscheduler.share.script;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.exception.ErrorContext;
import ru.mts.workflowscheduler.exception.ErrorLocation;
import ru.mts.workflowscheduler.exception.ScriptErrorContext;
import ru.mts.workflowscheduler.service.InputValidationContext;
import ru.mts.workflowscheduler.share.validation.Errors2;

@Data
@Accessors(chain = true)
public class ClientErrorDescription {
  private Errors2 error;
  private Object[] messageArgs;
  private Object[] adviceMessageArgs;
  private String systemMessage;

  private ErrorLocation location;
  private ErrorContext context;
  private ScriptErrorContext scriptContext;
  private InputValidationContext inputValidationContext;

  public ClientErrorDescription(Errors2 error) {
    this.error = error;
  }

  public ClientErrorDescription() {}

}
