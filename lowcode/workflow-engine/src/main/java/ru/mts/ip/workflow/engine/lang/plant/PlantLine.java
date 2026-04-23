package ru.mts.ip.workflow.engine.lang.plant;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlantLine {
  private String id;
  private JsonNode comment;
  private String lem;
  private boolean supportColoring;

  public PlantLine(String id, Object description, String lem) {
    this(id, PlantUtils.writeValueAsNode(description), lem, false);
  }

  public PlantLine(String id, Object description, String lem, boolean supportColoriong) {
    this(id, PlantUtils.writeValueAsNode(description), lem, supportColoriong);
  }
}
