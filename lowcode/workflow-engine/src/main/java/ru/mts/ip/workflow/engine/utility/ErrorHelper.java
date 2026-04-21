package ru.mts.ip.workflow.engine.utility;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.controller.dto.starter.ReqStopStarter;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ErrorMessagePouch;

import java.util.Optional;
import java.util.UUID;

@UtilityClass
public class ErrorHelper {

  public ClientError workerIsNotFound(UUID id) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKER_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id}));
  }

  public ClientError workerIsNotFound(UUID id, UUID executionId) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.WORKER_NOT_FOUND_BY_ID_AND_EXECUTION_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id, executionId}));
  }

  public static ClientError starterAlreadyExists(String name, String type) {
    return new ClientError(HttpStatus.CONFLICT, Errors2.STARTER_ALREADY_EXISTS, new ErrorMessagePouch().setMessageAargs(new Object[] {name, type}));
  }

  public static ClientError starterIsNotFound(UUID id) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.STARTER_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id}));
  }

  public static  ClientError starterIsNotFound(ReqStopStarter stop) {
    var name = Optional.ofNullable(stop).map(ReqStopStarter::getName).orElse(null);
    var type = Optional.ofNullable(stop).map(ReqStopStarter::getType).orElse(null);
    var workflowId = Optional.ofNullable(stop).map(ReqStopStarter::getWorkflowId).orElse(null);
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.STARTER_IS_NOT_FOUND_BY_STOP, new ErrorMessagePouch().setMessageAargs(new Object[] {name, type, workflowId}));
  }

  public static ClientError taskIsNotFound(UUID id) {
    return new ClientError(HttpStatus.NOT_FOUND, Errors2.STARTER_TASK_IS_NOT_FOUND_BY_ID, new ErrorMessagePouch().setMessageAargs(new Object[] {id}));
  }

  public static ClientError starterAlreadyDeleted(UUID starterId) {
    return new ClientError(HttpStatus.CONFLICT, Errors2.STARTER_ALREADY_DELETED, new ErrorMessagePouch().setMessageAargs(new Object[] {starterId}));
  }
}
