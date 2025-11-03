package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when report export operations fail.
 */
public class ExportException extends DomainException {
    
    public ExportException(String message) {
        super(message);
    }
    
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
