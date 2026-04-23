package ru.mts.workflowmail.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("serial")
public class ConstraintViolationException extends RuntimeException{
  private final List<ErrorDescription> errors;
}
