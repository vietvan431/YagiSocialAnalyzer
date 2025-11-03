package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Types of reports that can be generated.
 */
public enum ReportType {
    /** Sentiment analysis timeline and statistics */
    SENTIMENT_ANALYSIS,
    
    /** Damage distribution and breakdown */
    DAMAGE_CATEGORIZATION,
    
    /** Both sentiment and damage analysis */
    COMBINED
}
