package ru.mts.workflowmail.controller.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class Worker {
  private UUID id;
  private OffsetDateTime createTime;
  private OffsetDateTime changeTime;
  private UUID executorId;
  private OffsetDateTime lockedUntilTime;
  private Integer retryCount;
  private String status;
  private String errorMessage;
  private String errorStackTrace;
  private Starter starter;
}
