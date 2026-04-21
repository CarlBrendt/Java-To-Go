package ru.mts.ip.workflow.engine.exception;

import java.util.ArrayList;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.dto.InputValidationContext;
import ru.mts.ip.workflow.engine.dto.ScriptErrorContext;
import ru.mts.ip.workflow.engine.lang.InfinityCyclesContext;

@Data
@Accessors(chain = true)
public class ClientErrorDescription {
  private Errors2 error;
  private Object[] messageAargs;
  private Object[] adviceMessageArgs;
  private String systemMessage;
  
  private ErrorLocation location;
  private ErrorContext context;
  
  private ScriptErrorContext scriptContext;
  private InputValidationContext inputValidationContext;
  private InfinityCyclesContext infinityCyclesContext;
  
  public ClientErrorDescription(Errors2 error) {
    this.error = error;
  }
  
  public ClientErrorDescription() {}

  public void setParentLocation(ErrorLocation parentLocation) {
    if(location != null) {
      var path = location.getExecutionPath();
      var extendedPath = new ArrayList<>(parentLocation.getExecutionPath());
      extendedPath.addAll(path);
      location.setExecutionPath(extendedPath);
    } else {
      location = parentLocation;
    }
  }
  
  
  
}
