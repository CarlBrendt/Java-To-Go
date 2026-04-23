package ru.mts.workflowscheduler.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.mts.workflowscheduler.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.workflowscheduler.exception.ConstraintViolationException;
import ru.mts.workflowscheduler.exception.EntityNotFoundException;
import ru.mts.workflowscheduler.exception.WorkflowEngineException;
import ru.mts.workflowscheduler.mapper.DtoMapper;
import ru.mts.workflowscheduler.service.ErrorCompiler;
import ru.mts.workflowscheduler.share.script.ClientError;


import java.time.OffsetDateTime;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerExceptionHandler {

  private final DtoMapper mapper;
  private final ErrorCompiler errorCompiler;

  @ExceptionHandler(WorkflowEngineException.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(
      WorkflowEngineException ex, HttpServletRequest req) {
    var status = HttpStatus.BAD_REQUEST;
    var body = new ResCommonErrorWithDescriptions();
    body.setErrorDescriptions(mapper.toResErrorDescriptions(ex.getDescriptions()));
    body.setStatus(status.value());
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(
      ConstraintViolationException ex, HttpServletRequest req) {
    var status = HttpStatus.BAD_REQUEST;
    var body = new ResCommonErrorWithDescriptions();
    body.setErrorDescriptions(mapper.toResErrorDescriptions(ex.getErrors()));
    body.setStatus(status.value());
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }


  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleEntityNotFoundException(EntityNotFoundException ex,
      HttpServletRequest req) {
    var status = HttpStatus.NOT_FOUND;
    var body = new ResCommonErrorWithDescriptions();
    body.setStatus(status.value());
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(ClientError.class)
  public ResponseEntity<ResCommonErrorWithDescriptions> handleInvalidWorkflowDefinitionException(ClientError ex, HttpServletRequest req) {
    var status = ex.getStatus();
    var body = new ResCommonErrorWithDescriptions();
    body.setErrorDescriptions(mapper.toResErrorDescriptions(errorCompiler.toErrorDescription(ex)));
    body.setTimestamp(OffsetDateTime.now());
    body.setPath(req.getRequestURI());
    body.setError(status.getReasonPhrase());
    if(status != null) {
      body.setStatus(status.value());
    }
    return new ResponseEntity<>(body, status);
  }

}
