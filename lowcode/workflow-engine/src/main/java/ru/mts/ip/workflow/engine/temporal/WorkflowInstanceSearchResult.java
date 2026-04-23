package ru.mts.ip.workflow.engine.temporal;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WorkflowInstanceSearchResult {
  private String nextPageToken;
  private List<WorkflowInstanceSearchListValue> values;
  
  public void addValue(WorkflowInstanceSearchListValue value) {
    if(values == null) {
      values = new ArrayList<>();
    }
    values.add(value);
  }
}
