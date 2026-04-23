package ru.mts.ip.workflow.engine.lang.plant;

import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;

public class PlantUmlSyntaxError extends ClientError {
  
  private static final long serialVersionUID = 1L;

  public PlantUmlSyntaxError(Errors2 error, Object ...messageArgs) {
    super(error, new ErrorMessagePouch().setMessageAargs(messageArgs));
  }

  public PlantUmlSyntaxError(String systemMessage) {
    super(Errors2.INVALID_PLANT, new ErrorMessagePouch().setSystemMessage(systemMessage));
  }

}
