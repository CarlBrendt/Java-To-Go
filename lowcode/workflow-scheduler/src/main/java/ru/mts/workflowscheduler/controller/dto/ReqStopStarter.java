package ru.mts.workflowscheduler.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;


@Data
@Accessors(chain = true)
public class ReqStopStarter {
  private String name;
  private String tenantId;
  private UUID workflowId;
}
