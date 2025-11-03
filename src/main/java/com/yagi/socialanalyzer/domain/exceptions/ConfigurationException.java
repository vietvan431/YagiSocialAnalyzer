package com.yagi.socialanalyzer.domain.exceptions;

/**
 * Exception thrown when configuration loading or parsing fails.
 */
public class ConfigurationException extends DomainException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
