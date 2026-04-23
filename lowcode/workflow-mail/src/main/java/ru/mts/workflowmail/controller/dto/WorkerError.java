
package ru.mts.workflowmail.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import ru.mts.workflowmail.service.Const;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class WorkerError {
  private UUID workerId;
  private UUID executorId;
  private String errorMessage;
  private String stackTrace;

  public static WorkerError fromThrowable(UUID workerId, Throwable ex) {
    String message = ex != null ? ex.getMessage() : "Unknown error";
    String stackTrace = ex != null ? StringUtils.truncate(ExceptionUtils.getStackTrace(ex), 4000) : null;
    return new WorkerError(workerId, Const.getApplicationInstanceId(), message, stackTrace);
  }
}
