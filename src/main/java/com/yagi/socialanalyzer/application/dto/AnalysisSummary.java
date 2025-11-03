package com.yagi.socialanalyzer.application.dto;

import java.time.Instant;

/**
 * Overall analysis summary DTO combining collection, sentiment, and damage stats.
 */
public record AnalysisSummary(
    String projectId,
    String projectName,
    CollectionStatus collectionStatus,
    SentimentSummary sentimentSummary,
    DamageSummary damageSummary,
    Instant lastUpdated
) {
    public boolean isComplete() {
        return collectionStatus != null && 
               !collectionStatus.isActive() &&
               sentimentSummary != null &&
               sentimentSummary.totalAnalyzed() > 0;
    }
    
    public boolean hasDamageAnalysis() {
        return damageSummary != null && damageSummary.totalClassified() > 0;
    }
}
