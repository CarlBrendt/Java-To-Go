package ru.mts.workflowscheduler.service.dto;

import java.util.UUID;

public record ReplacedStarter(
    ShortStarter oldStarter, ShortStarter newStarter) {

  public record ShortStarter(
      UUID starterId,
      UUID workflowId
  ){}
}
