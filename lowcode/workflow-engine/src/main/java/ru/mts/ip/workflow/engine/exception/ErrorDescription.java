package ru.mts.ip.workflow.engine.exception;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.InputValidationContext;
import ru.mts.ip.workflow.engine.dto.ScriptErrorContext;
import ru.mts.ip.workflow.engine.dto.StarterErrorContext;
import ru.mts.ip.workflow.engine.lang.InfinityCyclesContext;

@Getter
@Setter
@Accessors(chain = true)
public class ErrorDescription {

  private String code;
  private String level;
  private String message;
  private String systemMessage;
  private String solvingAdviceMessage;
  private String solvingAdviceUrl;
  
  private StarterErrorContext starter;
  private ErrorLocation location;
  private ErrorContext  context;
  private ScriptErrorContext scriptContext;
  private InputValidationContext inputValidationContext;
  private InfinityCyclesContext infinityCyclesContext;
  private List<ErrorDescription> cause;
  
  @Override
  public String toString() {
    return "ErrorDescription [code=" + code + ", message=" + message + ", solvingAdviceMessage="
        + solvingAdviceMessage + ", solvingAdviceUrl=" + solvingAdviceUrl + ", location=" + location
        + ", context=" + context + "]";
  }
  
  public void setRootFieldPath(String path) {
    if(path != null && ! "$".equals(path)) {
      if(location != null) {
        var currentPath = location.getFieldPath();
        if(currentPath.length() > 2) {
          path = "%s.%s".formatted(path, currentPath.substring(2, currentPath.length()));
        }
        location.setFieldPath(path);
      }
    }
  }
  
  public void setParentLocation(ErrorLocation parentLocation) {
    if(location != null) {
      var path = location.getExecutionPath();
      var extendedPath = new ArrayList<>(parentLocation.getExecutionPath());
      extendedPath.add(parentLocation.getNextTransition());
      if(path != null) {
        extendedPath.addAll(path);
      }
      location.setExecutionPath(extendedPath);
    } else {
      location = parentLocation;
    }
  }
  
  public ErrorDescription truncate() {
    solvingAdviceMessage = null;
    solvingAdviceUrl = null;
    return this;
  }
}
