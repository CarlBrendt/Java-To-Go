package ru.mts.workflowscheduler.share.script;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import ru.mts.workflowscheduler.exception.ErrorDescription;
import ru.mts.workflowscheduler.exception.ErrorLocation;
import ru.mts.workflowscheduler.exception.ErrorMessagePouch;
import ru.mts.workflowscheduler.share.validation.Errors2;
import ru.mts.workflowscheduler.share.validation.schema.ConstraintViolation;

import java.util.List;
import java.util.Optional;

@Getter
public class ClientError extends RuntimeException{

  private HttpStatus status;

  private final List<ClientErrorDescription> errors;

  @Setter
  private List<ErrorDescription> compiledErrors;

  private ConstraintViolation constraintViolation;

  public ClientError(HttpStatus status, Errors2 error, ErrorMessagePouch messages) {
    this(List.of(new ClientErrorDescription().setError(error).setMessageArgs(messages.getMessageAargs())
        .setSystemMessage(messages.getSystemMessage()).setAdviceMessageArgs(messages.getAdviceMessageArgs())));
    this.status = status;
  }

  public ClientError(Errors2 error, ErrorMessagePouch messages) {
    this(HttpStatus.BAD_REQUEST, error, messages);
  }

  public ClientError(ConstraintViolation constraintViolation) {
    this.constraintViolation = constraintViolation;
    this.status = HttpStatus.BAD_REQUEST;
    errors = null;
  }

  public ClientError(@NonNull List<ClientErrorDescription> errors) {
    this.errors = errors;
    this.status = HttpStatus.BAD_REQUEST;
  }

  public ClientError(@NonNull ClientErrorDescription error) {
    this(List.of(error));
  }

  public void setLocation(ErrorLocation newLocation) {
    errors.forEach(e -> {
      var location = Optional.ofNullable(e.getLocation()).orElse(newLocation);
      location.setExecutionPath(newLocation.getExecutionPath());
      location.setFieldPath(newLocation.getFieldPath());
      location.setNextTransition(newLocation.getNextTransition());
      e.setLocation(location);
    });
  }

  public static ClientError compiled(List<ErrorDescription> errors) {
    ClientError result = new ClientError(List.of());
    result.compiledErrors = errors == null ? List.of() : errors;
    return result;
  }

}
