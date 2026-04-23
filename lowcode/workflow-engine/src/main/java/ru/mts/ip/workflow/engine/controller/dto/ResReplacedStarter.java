package ru.mts.ip.workflow.engine.controller.dto;

import java.util.UUID;

public record ResReplacedStarter(ResShortStarter oldStarter, ResShortStarter newStarter) {

  public record ResShortStarter(
      UUID starterId,
      UUID workflowId
  ){}
}
