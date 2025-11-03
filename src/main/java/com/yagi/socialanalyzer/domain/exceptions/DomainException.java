package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Base exception for all domain-related errors.
 * All domain exceptions should extend this class to maintain clear exception hierarchy.
 */
public class DomainException extends RuntimeException {
    
    public DomainException(String message) {
        super(message);
    }
    
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
