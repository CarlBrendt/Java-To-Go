package ru.mts.ip.workflow.engine.controller.dto.starter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.UUID;

public record WorkerError(UUID workerId,
                          UUID executorId,
                          String errorMessage,
                          String stackTrace) {

  public static WorkerError fromThrowable(UUID workerId, UUID executorId, Throwable ex) {
    String message = ex != null ? ex.getMessage() : "Unknown error";
    String stackTrace = ex != null ? StringUtils.truncate(ExceptionUtils.getStackTrace(ex), 4000) : null;
    return new WorkerError(workerId, executorId, message, stackTrace);
  }
}
