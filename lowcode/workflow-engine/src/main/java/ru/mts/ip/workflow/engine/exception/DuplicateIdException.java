package ru.mts.ip.workflow.engine.exception;

@SuppressWarnings("serial")
public class DuplicateIdException extends RuntimeException {
    public DuplicateIdException(String message) {
        super(message);
    }
}
