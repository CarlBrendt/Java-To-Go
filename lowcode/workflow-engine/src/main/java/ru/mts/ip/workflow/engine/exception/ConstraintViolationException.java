package ru.mts.ip.workflow.engine.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
@SuppressWarnings("serial")
public class ConstraintViolationException extends RuntimeException{
  
  private final HttpStatus httpStatus;
  private final List<ErrorDescription> errors;
  
  public ConstraintViolationException(String message, HttpStatus httpStatus, List<ErrorDescription> errors) {
    super(message);
    this.httpStatus = httpStatus;
    this.errors = errors;
  }
  public ConstraintViolationException(HttpStatus httpStatus, List<ErrorDescription> errors) {
    this("Constraint violation", httpStatus, errors);
  }

  public ConstraintViolationException(List<ErrorDescription> errors) {
    this(HttpStatus.BAD_REQUEST, errors);
  }
  
  
}