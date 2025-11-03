package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when sentiment or damage analysis operations fail.
 */
public class AnalysisException extends DomainException {
    
    public AnalysisException(String message) {
        super(message);
    }
    
    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
