package ru.mts.ip.workflow.engine.controller.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ResFoundDefinitionsByRef {
  private List<ResExecutableWorkflow> found = new ArrayList<>();
}
