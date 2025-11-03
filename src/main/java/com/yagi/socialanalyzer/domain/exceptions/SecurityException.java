package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when security operations (encryption, credential storage) fail.
 */
public class SecurityException extends DomainException {
    
    public SecurityException(String message) {
        super(message);
    }
    
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
