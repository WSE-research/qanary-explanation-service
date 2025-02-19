package com.wse.qanaryexplanationservice.exceptions;

/**
 * Base exception for errors occurring during explanation processing.
 */
public class ExplanationException extends Exception {

    public ExplanationException() {
        super();
    }

    public ExplanationException(String message) {
        super(message);
    }

    public ExplanationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExplanationException(Throwable cause) {
        super(cause);
    }
}