package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when repository operations fail.
 */
public class RepositoryException extends DomainException {
    
    public RepositoryException(String message) {
        super(message);
    }
    
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
