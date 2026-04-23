package ru.mts.ip.workflow.engine.controller;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerExceptionHandler {

  private final DtoMapper mapper;
  private final ErrorCompiler errorCompiler;

  @ExceptionHandler(ClientError.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(ClientError ex, HttpServletRequest req) {
    var status = ex.getStatus();
    var body = new ResCommonErrorWithDescriptions();
    body.setErrorDescriptions(mapper.toResErrorDescriptions(errorCompiler.toErrorDescription(ex)));
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }
  
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(
      ConstraintViolationException ex, HttpServletRequest req) {
    var status = Optional.ofNullable(ex.getHttpStatus()).orElse(HttpStatus.BAD_REQUEST);
    var body = new ResCommonErrorWithDescriptions();
    body.setErrorDescriptions(mapper.toResErrorDescriptions(ex.getErrors()));
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    body.setMessage(ex.getMessage());
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(TypeMismatchException.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(
      TypeMismatchException ex, HttpServletRequest req) {
    var body = new ResCommonErrorWithDescriptions();
    var status = HttpStatus.BAD_REQUEST;
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    body.setMessage(ex.getMessage());
    return new ResponseEntity<>(body, status);
  }

}
