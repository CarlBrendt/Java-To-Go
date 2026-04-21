package ru.mts.ip.workflow.engine.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EventCorrelation {
  private String businessKey;
  private String messageName;
}
