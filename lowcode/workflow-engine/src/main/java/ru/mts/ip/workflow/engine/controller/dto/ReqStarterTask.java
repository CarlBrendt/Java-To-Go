package ru.mts.ip.workflow.engine.controller.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.entity.SapTaskDetails;

@Data
public class ReqStarterTask {
  private String starterType;
  private SapTaskDetails sapTaskDetails;
}
