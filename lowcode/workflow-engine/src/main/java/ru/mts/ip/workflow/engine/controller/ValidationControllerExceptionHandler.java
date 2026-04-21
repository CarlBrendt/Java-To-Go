package ru.mts.ip.workflow.engine.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapper;
import ru.mts.ip.workflow.engine.controller.dto.ResCommonErrorWithDescriptions;
import ru.mts.ip.workflow.engine.controller.dto.ResWorkflowExpressionValidationResult;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ConstraintViolationException;
import ru.mts.ip.workflow.engine.validation.ErrorCompiler;

import java.time.OffsetDateTime;
import java.util.Optional;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = WorkflowValidationApiImpl.class)
@RequiredArgsConstructor
public class ValidationControllerExceptionHandler {

  private final DtoMapper mapper;
  private final ErrorCompiler errorCompiler;

  @ExceptionHandler(ClientError.class)
  public ResponseEntity<ResWorkflowExpressionValidationResult> handleInvalidWorkflowDefinitionException(ClientError ex, HttpServletRequest req) {
    var status = ex.getStatus() == HttpStatus.BAD_REQUEST ? HttpStatus.OK : ex.getStatus();
    var body = new ResWorkflowExpressionValidationResult();
    body.setErrors(mapper.toResErrorDescriptions(errorCompiler.toErrorDescription(ex)));
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResWorkflowExpressionValidationResult> handleInvalidWorkflowDefinitionException(
      ConstraintViolationException ex, HttpServletRequest req) {
    var status = Optional.ofNullable(ex.getHttpStatus()).orElse(HttpStatus.OK);
    status = status == HttpStatus.BAD_REQUEST ? HttpStatus.OK : status;
    var body = new ResWorkflowExpressionValidationResult();
    body.setErrors(mapper.toResErrorDescriptions(ex.getErrors()));
    return new ResponseEntity<>(body, status);
  }

}
