package ru.mts.ip.workflow.engine.validation;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;

@Data
@Accessors(chain = true)
public class ValidateDecision {
  
  private Errors2 error;
  private ErrorMessageArgs args;
  private List<ErrorDescription> errorDescriptions;
  private ValidateDecision cause;
  
  public boolean isError() {
    return error != null || errorDescriptions != null;
  }
  
  public ValidateDecision(Errors2 error){
    this.error = error;
  }

  public ValidateDecision(Errors2 error, ValidateDecision cause){
    this.cause = cause;
    this.error = error;
  }

  public ValidateDecision(){
    
  }

  public ValidateDecision(List<ErrorDescription> descriptions){
    errorDescriptions = descriptions;
  }
  
}
