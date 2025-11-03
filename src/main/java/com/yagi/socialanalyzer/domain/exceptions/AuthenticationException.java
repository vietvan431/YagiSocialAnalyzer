package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when authentication with external platforms fails.
 */
public class AuthenticationException extends DomainException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
