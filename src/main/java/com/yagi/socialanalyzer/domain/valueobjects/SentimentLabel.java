package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Sentiment classification labels.
 */
public enum SentimentLabel {
    /** Optimistic, hopeful, supportive content */
    POSITIVE,
    
    /** Fearful, angry, sad, critical content */
    NEGATIVE,
    
    /** Factual, informational, balanced content */
    NEUTRAL
}
