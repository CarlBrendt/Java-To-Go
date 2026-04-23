package ru.mts.ip.workflow.engine.controller;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResAsyncStartingResult {
  private String runId;
  private String businessKey;
}
