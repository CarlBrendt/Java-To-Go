package ru.mts.workflowmail.exception;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.mts.workflowmail.service.InputValidationContext;

import java.util.ArrayList;
import java.util.List;

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

  @Data
  @Accessors(chain = true)
  public static class InfinityCyclesContext {
    private List<List<String>> infinityCycles;
  }

}
