package com.yagi.socialanalyzer.application.dto;

import com.yagi.socialanalyzer.domain.valueobjects.SentimentLabel;

/**
 * Sentiment analysis summary DTO.
 */
public record SentimentSummary(
    String projectId,
    long totalAnalyzed,
    long positiveCount,
    long negativeCount,
    long neutralCount,
    double averageConfidence,
    SentimentLabel dominantSentiment
) {
    public double getPositivePercentage() {
        return totalAnalyzed > 0 ? (positiveCount * 100.0) / totalAnalyzed : 0.0;
    }
    
    public double getNegativePercentage() {
        return totalAnalyzed > 0 ? (negativeCount * 100.0) / totalAnalyzed : 0.0;
    }
    
    public double getNeutralPercentage() {
        return totalAnalyzed > 0 ? (neutralCount * 100.0) / totalAnalyzed : 0.0;
    }
}
