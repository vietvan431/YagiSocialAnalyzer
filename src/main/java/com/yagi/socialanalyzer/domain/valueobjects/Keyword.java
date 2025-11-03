package com.yagi.socialanalyzer.domain.valueobjects;

import java.util.Objects;

/**
 * Value object encapsulating a search keyword with validation.
 * Keywords must be non-empty and not exceed 100 characters.
 */
public record Keyword(String value) {
    
    public Keyword {
        Objects.requireNonNull(value, "Keyword cannot be null");
        
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be empty");
        }
        
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(
                "Keyword cannot exceed 100 characters (current: " + trimmed.length() + ")");
        }
        
        // Store trimmed value
        value = trimmed;
    }
    
    /**
     * Get normalized (lowercase) version of keyword for matching.
     *
     * @return lowercase keyword
     */
    public String normalized() {
        return value.toLowerCase();
    }
}
