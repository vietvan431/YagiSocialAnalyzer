package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when data collection from social media platforms fails.
 */
public class CollectionException extends DomainException {
    
    public CollectionException(String message) {
        super(message);
    }
    
    public CollectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
