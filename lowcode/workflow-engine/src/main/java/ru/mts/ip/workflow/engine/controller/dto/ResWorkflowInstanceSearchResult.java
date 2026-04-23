package ru.mts.ip.workflow.engine.controller.dto;

import java.util.List;
import lombok.Data;

@Data
public class ResWorkflowInstanceSearchResult {
  private String nextPageToken;
  private List<ResWorkflowInstanceSearchListValue> values;
}
