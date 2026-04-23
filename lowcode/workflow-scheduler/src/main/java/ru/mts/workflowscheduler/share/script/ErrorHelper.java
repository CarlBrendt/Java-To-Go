package ru.mts.workflowscheduler.share.script;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import ru.mts.workflowscheduler.exception.ErrorMessagePouch;
import ru.mts.workflowscheduler.share.validation.Errors2;

import java.util.UUID;

@UtilityClass
public class ErrorHelper {

  public ClientError starterIsNotFound(UUID id) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.SCHEDULER_STARTER_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id}));
  }

  public ClientError workerIsNotFound(UUID id) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.SCHEDULER_WORKER_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id}));
  }

  public ClientError starterAlreadyExists(String name, String tenantId) {
    return new ClientError(HttpStatus.CONFLICT, Errors2.SCHEDULER_STARTER_ALREADY_EXISTS, new ErrorMessagePouch().setMessageAargs(new Object[] {name, tenantId}));
  }


}
