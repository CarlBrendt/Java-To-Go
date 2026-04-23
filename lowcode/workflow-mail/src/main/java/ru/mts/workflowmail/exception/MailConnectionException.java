package ru.mts.workflowmail.exception;

public class MailConnectionException extends Exception {

  public MailConnectionException(String message) {
    super(message);
  }
  public MailConnectionException(Throwable e) {
    super(e);
  }
}
