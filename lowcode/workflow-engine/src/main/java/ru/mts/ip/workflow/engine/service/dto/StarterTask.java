package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.entity.SapTaskDetails;

@Data
public class StarterTask {
  private SapTaskDetails sapTaskDetails;
}
