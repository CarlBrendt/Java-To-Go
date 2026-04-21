package ru.mts.ip.workflow.engine.exception;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorLocation {

  private String fieldPath;
  private String activityId;
  private List<String> executionPath;
  private String nextTransition;
  
  public List<String> nextTransitionState(){
    List<String> res = new ArrayList<>(executionPath);
    res.add(nextTransition);
    return res;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ErrorLocation(");
    if(fieldPath != null) {
      sb.append(fieldPath);
    } else {
      sb.append(String.join(".", executionPath));
      sb.append(" -> ").append(nextTransition);
    }
    sb.append(")");
    return sb.toString();
  }
  
}
