package ru.mts.ip.workflow.engine.controller.dto.starter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.UUID;

import static ru.mts.ip.workflow.engine.Const.WorkerStatus.ERROR;
import static ru.mts.ip.workflow.engine.Const.WorkerStatus.SCHEDULED_TO_START;
import static ru.mts.ip.workflow.engine.Const.WorkerStatus.STARTED;

@Data
@Accessors(chain = true)
public class WorkerLocker {
  @NotNull
  private UUID executorId;
  @NotNull
  @NotEmpty
  private String type;
  private Set<String> statuses = Set.of(ERROR, SCHEDULED_TO_START, STARTED);
}
