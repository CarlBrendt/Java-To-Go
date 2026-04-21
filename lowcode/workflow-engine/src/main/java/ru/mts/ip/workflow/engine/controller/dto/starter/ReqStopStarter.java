package ru.mts.ip.workflow.engine.controller.dto.starter;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;


@Data
@Accessors(chain = true)
public class ReqStopStarter {
  @NotNull
  private String type;
  @NotNull
  private String name;
  @NotNull
  private String tenantId;
  @NotNull
  private UUID workflowId;
}
