package ru.mts.ip.workflow.engine.service;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ActivityMultiInstanceState {
  private List<JsonNode> resultCollection;
  private String resultCollectionRef;
  private Integer completeCount;
  private Integer offset;
  private Integer size;
}
