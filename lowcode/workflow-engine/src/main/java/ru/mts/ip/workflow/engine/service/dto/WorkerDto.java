package ru.mts.ip.workflow.engine.service.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.Starter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class WorkerDto {
  private UUID id;
  private OffsetDateTime createTime;
  private OffsetDateTime changeTime;
  private UUID executorId;
  private Starter starter;
  private OffsetDateTime lockedUntilTime;
  private OffsetDateTime overdueTime;
  private Integer retryCount;
  private String status;
  private String errorMessage;
  private String errorStackTrace;
}
